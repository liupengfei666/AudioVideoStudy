package com.lewis.audiovideostudy.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.lewis.audiovideostudy.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 注意：这儿有一个小bug,第一次先用TextureView预览，不可以切换到SurfaceView，是因为点击SurfaceView预览时也会走onSurfaceTextureAvailable回调
 * 这儿我没有去修改，不影响。
 */
public class CameraPreviewActivity extends AppCompatActivity {

    private final String[] permission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private FrameLayout mContainer;
    private SurfaceView mSurfaceView;
    private TextureView mTextureView;

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;

    private CameraManager mCameraManager; //相机管理类
    private String mCameraId; //打开相机的id
    private Size mPreviewSize;//预览尺寸

    private CameraDevice mCameraDevice; //相机设备,具体的摄像头
    private CameraCaptureSession mCameraCaptureSession;//捕获的会话Session

    //是否是SurfaceView预览
    private boolean isSurfaceView = true;

    private ImageReader mImageReaderPreview;
    private File mFile; //图像保存的路径

    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        mContainer = findViewById(R.id.container);

        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        mSurfaceView = new SurfaceView(this);
        mTextureView = new TextureView(this);

        ActivityCompat.requestPermissions(this, permission, 100);
        mFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "pic.jpg");
    }

    /**
     * SurfaceView预览
     */
    public void surfaceViewPreview(View view) {
        stopPreview();
        mTextureView.setSurfaceTextureListener(null);
        mContainer.removeAllViews();
        mContainer.addView(mSurfaceView);
        mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
        mSurfaceView.setKeepScreenOn(true);
        isSurfaceView = true;
    }

    /**
     * TextureView预览
     */
    public void textureViewPreview(View view) {
        stopPreview();
        mSurfaceView.getHolder().addCallback(null);
        mContainer.removeAllViews();
        mContainer.addView(mTextureView);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        mTextureView.setKeepScreenOn(true);
        isSurfaceView = false;
    }

    /**
     * 初始化执行camera的线程和handler
     */
    private void startCameraThread() {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    /**
     * 停止camera线程和其handler
     */
    private void stopCameraThread() {
        mCameraThread.quitSafely();
        try {
            mCameraThread.join();
            mCameraThread = null;
            mCameraHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 得到相机id和预览大小
     */
    private void generalCameraIdAndSize(int width, int height) {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                //查询摄像头属性
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //默认打开后置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                //获取StreamConfigurationMan,它管理着摄像头支持的输出格式和尺寸
                StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (configurationMap == null) {
                    continue;
                }
                //获取预览尺寸
                mPreviewSize = chooseOptimalSize(configurationMap.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraId = cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 选择合适的预览大小
     *
     * @param outputSizes 输出size数组
     * @param width       容器宽
     * @param height      容器高
     * @return size
     */
    private Size chooseOptimalSize(Size[] outputSizes, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size size : outputSizes) {
            if (width > height) {
                if (size.getWidth() > width && size.getHeight() > height) {
                    sizeList.add(size);
                }
            } else {
                if (size.getWidth() > height && size.getHeight() > width) {
                    sizeList.add(size);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                }
            });
        }
        return outputSizes[0];
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            mCameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建时的状态回调
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            //在开启预览前设置
            setupImageReader();
            if (isSurfaceView) {
                startSurfaceViewPreview();
            } else {
                startTextureViewPreview();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            finish();
        }
    };

    /**
     * 设置ImageReader
     */
    private void setupImageReader() {
        //前面三个参数分别是需要的尺寸和格式，最后一个是一次获取几帧数据
        mImageReaderPreview = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 2);
        //监听ImageReader事件，当有可用的图像流数据时回调，它的参数就是预览帧数据
        mImageReaderPreview.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //获取捕获的照片数据
                Image image = reader.acquireLatestImage();
                mCameraHandler.post(new ImageSaver(image, mFile));
            }
        }, null);
    }

    /**
     * SurfaceView开始预览
     */
    private void startSurfaceViewPreview() {
        //获取输出的surface
        Surface surface = mSurfaceView.getHolder().getSurface();
        try {
            final CaptureRequest.Builder captureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //添加输出的surface
            captureRequest.addTarget(surface);
            captureRequest.addTarget(mImageReaderPreview.getSurface());
            //创建CameraCaptureSession时加上mImageReaderPreview.getSurface(),这样预览数据就同时输出到两个surface了
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReaderPreview.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                    //自动对焦
                    captureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    CaptureRequest request = captureRequest.build();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(request, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * TextureView开始预览
     */
    private void startTextureViewPreview() {
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);
        try {
            final CaptureRequest.Builder captureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequest.addTarget(surface);
            captureRequest.addTarget(mImageReaderPreview.getSurface());
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReaderPreview.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                    CaptureRequest request = captureRequest.build();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(request, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止预览
     */
    private void stopPreview() {
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReaderPreview != null) {
            mImageReaderPreview.close();
            mImageReaderPreview = null;
        }
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.w("preview", "surfaceCreated");
            generalCameraIdAndSize(holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
            openCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.w("preview", "surfaceDestroyed");
            stopPreview();
        }
    };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.w("preview", "onSurfaceTextureAvailable");
            generalCameraIdAndSize(width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.w("preview", "onSurfaceTextureDestroyed");
            stopPreview();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        startCameraThread();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPreview();
        stopCameraThread();
    }

    /**
     * 拍照，点击之前请先预览
     */
    public void takePhoto(View view) {
        //创建请求拍照的CaptureRequest
        try {
            final CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //设置CaptureRequest输出到ImageReader
            captureRequestBuilder.addTarget(mImageReaderPreview.getSurface());
            if (isSurfaceView) {
                captureRequestBuilder.addTarget(mSurfaceView.getHolder().getSurface());
            } else {
                captureRequestBuilder.addTarget(new Surface(mTextureView.getSurfaceTexture()));
            }
            //设置拍照方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            //拍照完成后重启预览，因为拍照后会导致预览停止
            final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    //重启预览
                    try {
                        mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mCameraHandler);
                        Toast.makeText(CameraPreviewActivity.this, "拍照成功", Toast.LENGTH_SHORT).show();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            };
            //停止预览
            mCameraCaptureSession.stopRepeating();
            //调用拍照方法
            mCameraCaptureSession.capture(captureRequestBuilder.build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            //我们可以将这帧数据转成字节数组，类似于Camera1的PreviewCallback回调的预览帧数据
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
