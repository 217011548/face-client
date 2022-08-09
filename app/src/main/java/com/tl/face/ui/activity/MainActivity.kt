package com.tl.face.ui.activity

import android.app.AlertDialog
import android.app.smdt.SmdtManager
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Environment
import android.os.Handler
import android.text.InputType
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.tl.base.BaseDialog
import com.hjq.http.EasyHttp
import com.hjq.http.listener.OnUpdateListener
import com.hjq.permissions.Permission
import com.hjq.toast.ToastUtils.setView
import com.tl.face.AppObject
import com.tl.face.R
import com.tl.face.aop.Log
import com.tl.face.aop.Permissions
import com.tl.face.app.AppActivity
import com.tl.face.callback.OnErrorCallback
import com.tl.face.callback.OnRecognizeCallback
import com.tl.face.configuration.*
import com.tl.face.constant.FaceErrorType
import com.tl.face.face.FaceActive
import com.tl.face.http.api.UpdateImageApi
import com.tl.face.http.model.HttpData
import com.tl.face.manager.SoundPoolManager
import com.tl.face.manager.SoundType
import com.tl.face.ui.dialog.MessageDialog
import com.tl.face.utils.LogUtil.d
import com.tl.face.utils.NV21Util
import com.tl.face.view.FaceCameraView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.f1reking.serialportlib.SerialPortHelper
import me.f1reking.serialportlib.listener.IOpenSerialPortListener
import me.f1reking.serialportlib.listener.ISerialPortDataListener
import me.f1reking.serialportlib.listener.Status
import okhttp3.Call
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import timber.log.Timber
import android.view.KeyEvent.KEYCODE_SEARCH
import android.view.inputmethod.EditorInfo
import android.widget.TextView


class MainActivity : AppActivity() {
  //preview page
  private val faceCameraView: FaceCameraView? by lazy { findViewById(R.id.face_camera_view) }
  //draw the body frame
  private val statusImageView: AppCompatImageView? by lazy { findViewById(R.id.main_status_image_view) }
  //init serial port
  private val serialHelper: SerialPortHelper by lazy {
    SerialPortHelper.Builder(
      "/dev/ttyS3",
      115200
    ).build()
  }

    private val smdtManager: SmdtManager? by lazy {
    null
    SmdtManager.create(this)
  }

  private val mScope: CoroutineScope by lazy { MainScope() }
  //display temperature TextView
  private val temperatureTextView: AppCompatTextView? by lazy { findViewById(R.id.main_temperature_text_view) }
  //display screen saver ImageView
  private val screenSaverImageView: AppCompatImageView? by lazy { findViewById(R.id.main_screen_saver_image_view) }
  //init the value for temp
  private var currentUserTemp = 0.0
  //shutdown screen Runnable
  private val autoCloseScreenRunnable = Runnable {
    if (screenSaverImageView?.visibility == View.GONE) {
      screenSaverImageView?.visibility = View.VISIBLE
    }
  }
  //restrat to detect the face Runnable
  private val restartDetectionFaceRunnable = Runnable {
    faceCameraView?.pauseDetectionFace(false)
  }

  override fun getLayoutId(): Int {
    return R.layout.activity_main
  }

  override fun initView() {
  }

  override fun initData() {
    //init the Sound player
    SoundPoolManager.initHelper(this)
    activateSDK()
    serialHelper?.setIOpenSerialPortListener(object : IOpenSerialPortListener {
      override fun onSuccess(device: File?) {
        println("Serial Port Open Success")
      }

      override fun onFail(device: File?, status: Status?) {
        println("Serial Port Open Fail")
      }
    })
    serialHelper?.setISerialPortDataListener(object : ISerialPortDataListener {
      override fun onDataReceived(bytes: ByteArray?) {
        bytes?.let {
          post {
            if (it.size > 4 && it[0] == 0xA5.toByte() && it[1] == 0x55.toByte()) {
              removeCallbacks(restartDetectionFaceRunnable)
              //calculate the temperature
              currentUserTemp = (it[2] + 256 * it[3]) / 100.0
              println("temperature：$currentUserTemp")
              temperatureTextView?.text = "$currentUserTemp°C"
              if (currentUserTemp > 37.2) {
                //If over hear sound play failed
                SoundPoolManager.playSound(SoundType.Failed)
                temperatureTextView?.setTextColor(Color.RED)
                LEDRed()
                postDelayed(restartDetectionFaceRunnable, 3 * 1000)
              } else {
                showEditTextDialog()
//                SoundPoolManager.playSound(SoundType.Pass)
                temperatureTextView?.setTextColor(Color.GREEN)
              }
              //time out 3 sec
              //postDelayed(restartDetectionFaceRunnable, 3 * 1000)
            } else {
              temperatureTextView?.text = ""
            }
          }
        }
      }

      override fun onDataSend(bytes: ByteArray?) {
      }
    })
    serialHelper?.open()
  }

  override fun onDestroy() {
    super.onDestroy()
    mScope?.cancel()
  }


  fun LEDGreen(){
    //temperatureTextView?.setTextColor(Color.RED)
    smdtManager?.setLedLighted(SmdtManager.LED_GREEN, true)
    Handler().postDelayed({
    smdtManager?.setLedLighted(SmdtManager.LED_GREEN, false)
    }, 3000)
  }

  fun LEDRed(){
    //temperatureTextView?.setTextColor(Color.RED)
    smdtManager?.setLedLighted(SmdtManager.LED_RED, true)
    Handler().postDelayed({
      smdtManager?.setLedLighted(SmdtManager.LED_RED, false)
    }, 3000)
  }


  fun findMatch(s: String, strings: List<String>): String? {
    for (substring in strings) {
      if (s.contains(substring)) {
        return substring
      }
    }
    return null
  }

  // Display the dialog let user press the card
  fun showEditTextDialog() {

    val builder: AlertDialog.Builder = android.app.AlertDialog.Builder(this)
    builder.setTitle("Card Identify")

    val dialogLayout = layoutInflater.inflate(R.layout.edit_text_layout, null)
    val cardvalue = dialogLayout.findViewById<EditText>(R.id.et_presscard)
    builder.setView(dialogLayout)

    val closedialog = builder.show()

    cardvalue.setOnKeyListener { v, keyCode, event ->

      when {

        //Check if it is the Enter-Key,      Check if the Enter Key was pressed down
        ((keyCode == KeyEvent.KEYCODE_ENTER) && (event.action == KeyEvent.ACTION_DOWN)) -> {

          //perform an action here e.g. a send message button click

          val m_Text = cardvalue.text.toString()
          val hello = findViewById<TextView>(R.id.main_hello)

          println("ICCard$m_Text")

          val register = listOf("327369" , "001817", "#129775")

          var isexist = findMatch(m_Text, register)

          if (isexist != null){


            hello?.setText("Welcome")
            hello?.setTextColor(Color.GREEN)
            SoundPoolManager.playSound(SoundType.Pass)
            LEDGreen()


          } else {
            hello?.setText("Not Register ! ")
            LEDRed()
            hello?.setTextColor(Color.RED)
          }


          closedialog.dismiss()
          postDelayed(restartDetectionFaceRunnable, 3 * 1000)

          Handler().postDelayed({
            hello?.setText("")
          }, 3000)

          //return true
          return@setOnKeyListener true
        }
        else -> false
      }


    }


  }




  /**
   * Init Face Detect engine SDK
   */
  @Permissions(
    Permission.CAMERA, Permission.READ_PHONE_STATE, Permission.WRITE_SETTINGS,
    Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE
  )
  private fun activateSDK() {
    val response = FaceEngine.activeOnline(
      this,
      AppObject.FACE_ACTIVE_KEY,
      AppObject.FACE_ID,
      AppObject.FACE_KEY
    )
    when (response) {
      ErrorInfo.MERR_ASF_ALREADY_ACTIVATED, ErrorInfo.MOK -> {
        showSnackTip("sdk activated")
        // init the Camera
        initCamera()
      }
      else -> {
        MessageDialog.Builder(this)
          .setMessage("SDK activate fail，please try again，code:$response")
          .setListener(object : MessageDialog.OnListener {
            override fun onConfirm(dialog: BaseDialog?) {
              activateSDK()
            }
          })
          .hideCancel()
          .show()
      }
    }
  }

  //init the camera function
  private fun initCamera() {
    val config = FaceConfiguration.builder(this, object : OnRecognizeCallback {
      override fun nobody() {
        post {
          temperatureTextView?.text = ""
          println("no body")
          statusImageView?.setImageResource(R.drawable.ic_normal)
          removeCallbacks(autoCloseScreenRunnable)
          postDelayed(autoCloseScreenRunnable, 60 * 1000)
        }
      }

      override fun detectionFace(nv21: ByteArray, maskInfo: Int, width: Int, height: Int) {
        //Stop detect the face
        post {

          temperatureTextView?.text = ""
          removeCallbacks(autoCloseScreenRunnable)
          if (screenSaverImageView?.visibility == View.VISIBLE) {
            screenSaverImageView?.visibility = View.GONE
          }
          statusImageView?.setImageResource(R.drawable.ic_normal)

          faceCameraView?.pauseDetectionFace(true)
          when (maskInfo) {
            1 -> {
              currentUserTemp = 0.0
              //if masked
              statusImageView?.setImageResource(R.drawable.ic_green)
              //send command to start detect temperature
              //0xA5 0x55 0x01 0xFB
              val bytes = byteArrayOf(0xA5.toByte(), 0x55.toByte(), 0x01.toByte(), 0xFB.toByte())
              serialHelper?.sendBytes(bytes)
              //showEditTextDialog()
              println("send detect temperature command")
              postDelayed(restartDetectionFaceRunnable, 5 * 1000)
            }
            else -> {
              //No Mask
              statusImageView?.setImageResource(R.drawable.ic_red)
              LEDRed()
              //convert the photo and upload to server
              SoundPoolManager.playSound(SoundType.Failed)
              mScope.launch {
                val convertTask = convertNV21ToJpeg(nv21, width, height)
                if (convertTask) {
                  //upload to server
                  uploadImage()
                }
              }
            }
          }
          println(if (maskInfo == 0) "No Mask" else "Mask")
        }
      }

      override fun someone() {
//        post {
//          println("someone")
//          temperatureTextView?.text = ""
//          removeCallbacks(autoCloseScreenRunnable)
//          if (screenSaverImageView?.visibility == View.VISIBLE) {
//            screenSaverImageView?.visibility = View.GONE
//          }
//          statusImageView?.setImageResource(R.drawable.ic_normal)
//        }
      }

      override fun notInTheArea() {
        post {
          println("notInTheArea")
          temperatureTextView?.text = ""
          statusImageView?.setImageResource(R.drawable.ic_normal)
        }
      }
    })
      .setDetectFaceOrient(DetectFaceOrientPriority.ASF_OP_270_ONLY)
      .enableRecognize(false)
      .setLivenessType(LivenessType.RGB)
      .setRgbLivenessThreshold(0.6f)
      .setFqLivenessThreshold(0.6f)
      .setIrLivenessThreshold(0.7f)
      .enableImageQuality(false)
      .setDetectFaceMaxNum(1)
      .recognizeKeepMaxFace(true)
      .enableRecognizeAreaLimited(true)
      .setRecognizeAreaLimitedRatio(0.705f)
      .setDetectInfo(DetectInfo(age = false, gender = false))
      .setRgbCameraFcing(CameraFacing.BACK)
      .setPreviewSize(null)
      .setDrawFaceRect(DrawFaceRect(false))
      .isRgbMirror(false)
      .isIrMirror(false)
      .setExtractFeatureErrorRetryCount(999)
      .setRecognizeFailedRetryInterval(500)
      .setLivenessErrorRetryCount(999)
      .setLivenessFailedRetryInterval(500)
      .enableCompareFace(false)
      .enableMask(true)
      .setFaceSizeLimit(150)
      .setImageQualityNoMaskRecognizeThreshold(0.29f)
      .setImageQualityMaskRecognizeThreshold(0.29f)
      .setOnErrorCallback(object : OnErrorCallback {
        override fun onError(type: FaceErrorType, errorCode: Int, errorMessage: String) {

        }
      })
      .build()
    faceCameraView?.setConfiguration(config, FaceActive.isActivated(this), true)
    faceCameraView?.setLifecycleOwner(this)
  }

  //NV21 to JPG
  private suspend fun convertNV21ToJpeg(data: ByteArray, width: Int, height: Int): Boolean {
    //Save the image
    val fileName = "IMG_temp.jpg" //jpeg file name definition
    val sdRoot = getExternalFilesDir(Environment.MEDIA_MOUNTED) //system path
    val dir = "/jpeg/" //file name
    val mkDir = File(sdRoot, dir)
    if (!mkDir.exists()) {
      mkDir.mkdirs() //if path not exist just create
    }

    val pictureFile = File(sdRoot, dir + fileName)
    try {
      pictureFile.createNewFile()
      val filecon = FileOutputStream(pictureFile)
      val rotateData = NV21Util.rotateYUV420Degree270(data, width, height)
      val image = YuvImage(rotateData, ImageFormat.NV21, height, width,null) //makeNV21 data save to YuvImage
      //image compress
      image.compressToJpeg(
        Rect(0, 0, image.width, image.height),
        70, filecon
      )
    } catch (e: IOException) {
      e.printStackTrace()
      return false
    }
    return true
  }

  //uploadImage
  private fun uploadImage() {
    val api = UpdateImageApi()
      .setImage(File("${getExternalFilesDir(Environment.MEDIA_MOUNTED)}/jpeg/IMG_temp.jpg"))
    EasyHttp.post(this)
      .api(api)
      .request(object : OnUpdateListener<HttpData<String>> {
        override fun onSucceed(result: HttpData<String>?) {
          showSnackTip("Upload Image Success")
        }

        override fun onFail(e: Exception?) {
          showSnackTip("Upload Image fail")
        }

        override fun onProgress(progress: Int) {
        }

        override fun onEnd(call: Call?) {
          //resumed after 3 sec
          postDelayed(restartDetectionFaceRunnable, 3 * 1000)
        }
      })
  }

}


