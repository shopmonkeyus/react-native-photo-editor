package com.reactnativephotoeditor.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.reactnativephotoeditor.PhotoEditorModule
import com.reactnativephotoeditor.R
import com.reactnativephotoeditor.activity.StickerFragment.StickerListener
import com.reactnativephotoeditor.activity.constant.ResponseCode
import com.reactnativephotoeditor.activity.filters.FilterListener
import com.reactnativephotoeditor.activity.filters.FilterViewAdapter
import com.reactnativephotoeditor.activity.tools.EditingToolsAdapter
import com.reactnativephotoeditor.activity.tools.EditingToolsAdapter.OnItemSelected
import com.reactnativephotoeditor.activity.tools.ToolType
import ja.burhanrashid52.photoeditor.*
import ja.burhanrashid52.photoeditor.PhotoEditor.OnSaveListener
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType
import java.io.File


open class PhotoEditorActivity : AppCompatActivity(), OnPhotoEditorListener, View.OnClickListener,
    PropertiesBSFragment.Properties, ShapeBSFragment.Properties, StickerListener, OnItemSelected,
    FilterListener {
    private var mPhotoEditor: PhotoEditor? = null
    private var mProgressDialog: ProgressDialog? = null
    private var mPhotoEditorView: PhotoEditorView? = null
    private var mPropertiesBSFragment: PropertiesBSFragment? = null
    private var mShapeBSFragment: ShapeBSFragment? = null
    private var mShapeBuilder: ShapeBuilder? = null
    private var mStickerFragment: StickerFragment? = null
    private var mRvTools: RecyclerView? = null
    private var mRootView: ConstraintLayout? = null
    private var translations: ArrayList<String> = arrayListOf(
        "Shape",
        "Brush",
        "Opacity",
        "Line",
        "Oval",
        "Rectangle",
    );
    private var mEditingToolsAdapter: EditingToolsAdapter? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo_editor_view)
        initViews()

        //intern
        val value = intent.extras
        val path = value?.getString("path")
        val stickers = value?.getStringArrayList("stickers")?.plus(
            assets.list("Stickers")!!
                .map { item -> "/android_asset/Stickers/$item" }) as ArrayList<String>

        val rnTranslations = value?.getStringArrayList("translations") as ArrayList<String>

        if (!rnTranslations.isNullOrEmpty() && rnTranslations.size == 6) {
            translations = rnTranslations
        }

        mEditingToolsAdapter = EditingToolsAdapter(this)

//    println("stickers: $stickers ${stickers.size}")
//    for (stick in stickers) {
//      print("stick: $stickers")
//    }

        mPropertiesBSFragment = PropertiesBSFragment()
        mPropertiesBSFragment!!.setPropertiesChangeListener(this)

        mStickerFragment = StickerFragment()
        mStickerFragment!!.setStickerListener(this)

//    val stream: InputStream = assets.open("image.png")
//    val d = Drawable.createFromStream(stream, null)
        mStickerFragment!!.setData(stickers)

        mShapeBSFragment = ShapeBSFragment(translations)
        mShapeBSFragment!!.setPropertiesChangeListener(this)

        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvTools!!.layoutManager = llmTools

        if (mEditingToolsAdapter != null) {
            mRvTools!!.adapter = mEditingToolsAdapter
        }

        val pinchTextScalable = intent.getBooleanExtra(PINCH_TEXT_SCALABLE_INTENT_KEY, true)
        mPhotoEditor = PhotoEditor.Builder(this, mPhotoEditorView)
            .setPinchTextScalable(pinchTextScalable) // set flag to make text scalable when pinch
            .build() // build photo editor sdk
        mPhotoEditor?.setOnPhotoEditorListener(this)
//    val drawable = Drawable.cre

        Glide.with(this).load(path).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean
            ): Boolean {
                val intent = Intent()
                intent.putExtra("path", path)
                setResult(ResponseCode.LOAD_IMAGE_FAILED, intent)
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                //
                return false
            }
        })
//      .placeholder(drawable)
            .into(mPhotoEditorView!!.source);
    }

    private fun sendEventToRN(name: String) {
        val reactInstanceManager: ReactInstanceManager =
            (applicationContext as ReactApplication).reactNativeHost.reactInstanceManager

        val params = Arguments.createMap()
        params.putString(EVENT_EMITTER_DATA, name)

        reactInstanceManager.currentReactContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            ?.emit(EVENT_EMITTER_EVENT, params)
    }

    private fun showLoading(message: String) {
        mProgressDialog = ProgressDialog(this)
        mProgressDialog!!.setMessage(message)
        mProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.show()
    }

    protected fun hideLoading() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    private fun requestPermission(permission: String) {
        val isGranted =
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            ActivityCompat.requestPermissions(
                this, arrayOf(permission), READ_WRITE_STORAGE
            )
        }
    }

    private fun makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun initViews() {
        //REDO
        val imgRedo: ImageView = findViewById(R.id.imgRedo)
        imgRedo.setOnClickListener(this)
        //UNDO
        val imgUndo: ImageView = findViewById(R.id.imgUndo)
        imgUndo.setOnClickListener(this)
        //CLOSE
        val imgClose: ImageView = findViewById(R.id.imgClose)
        imgClose.setOnClickListener(this)
        //SAVE
        val btnSave: ImageView = findViewById(R.id.btnSave)
        btnSave.setOnClickListener(this)

        mPhotoEditorView = findViewById(R.id.photoEditorView)
        mRvTools = findViewById(R.id.rvConstraintTools)
        mRootView = findViewById(R.id.rootView)
    }

    override fun onEditTextChangeListener(rootView: View, text: String, colorCode: Int) {
        val textEditorDialogFragment = TextEditorDialogFragment.show(this, text, colorCode)
        textEditorDialogFragment.setOnTextEditorListener { inputText: String?, newColorCode: Int ->
            val styleBuilder = TextStyleBuilder()
            styleBuilder.withTextColor(newColorCode)
            mPhotoEditor!!.editText(rootView, inputText, styleBuilder)
        }
    }

    override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
        Log.d(
            TAG,
            "onAddViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )
    }

    override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
        Log.d(
            TAG,
            "onRemoveViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )
    }

    override fun onStartViewChangeListener(viewType: ViewType) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onStopViewChangeListener(viewType: ViewType) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [$viewType]")
    }

    @SuppressLint("NonConstantResourceId")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgUndo -> {
                mPhotoEditor!!.undo()
            }

            R.id.imgRedo -> {
                mPhotoEditor!!.redo()
            }

            R.id.btnSave -> {
                saveImage()
            }

            R.id.imgClose -> {
                onBackPressed()
            }
        }
    }

    private fun isSdkHigherThan28(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    private fun saveImage() {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val hasStoragePermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasStoragePermission || isSdkHigherThan28()) {
            showLoading("Saving...")
            val path: File = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val file = File(path, fileName)
            path.mkdirs()

            mPhotoEditor!!.saveAsFile(file.absolutePath, object : OnSaveListener {
                override fun onSuccess(@NonNull imagePath: String) {
                    sendEventToRN("Done")
                    hideLoading()
                    val intent = Intent()
                    intent.putExtra("path", imagePath)
                    setResult(ResponseCode.RESULT_OK, intent)
                    finish()
                }

                override fun onFailure(@NonNull exception: Exception) {
                    hideLoading()
                    if (!hasStoragePermission) {
                        requestPer()
                    } else {
                        mPhotoEditorView?.let {
                            val snackBar = Snackbar.make(
                                it, R.string.save_error, Snackbar.LENGTH_SHORT
                            )
                            snackBar.setBackgroundTint(Color.WHITE)
                            snackBar.setActionTextColor(Color.BLACK)
                            snackBar.setAction("Ok", null).show()
                        }
                    }
                }
            })
        } else {
            requestPer()
        }
    }

    private fun requestPer() {
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun onColorChanged(colorCode: Int) {
        mPhotoEditor!!.setShape(mShapeBuilder!!.withShapeColor(colorCode))
    }

    override fun onOpacityChanged(opacity: Int) {
        mPhotoEditor!!.setShape(mShapeBuilder!!.withShapeOpacity(opacity))
    }

    override fun onShapeSizeChanged(shapeSize: Int) {
        mPhotoEditor!!.setShape(mShapeBuilder!!.withShapeSize(shapeSize.toFloat()))
    }

    override fun onShapePicked(shapeType: ShapeType) {
        mPhotoEditor!!.setShape(mShapeBuilder!!.withShapeType(shapeType))
    }

    override fun onStickerClick(bitmap: Bitmap) {
        mPhotoEditor!!.addImage(bitmap)
    }

    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.msg_save_image))
        builder.setPositiveButton("Save") { _: DialogInterface?, _: Int -> saveImage() }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        builder.setNeutralButton("Discard") { _: DialogInterface?, _: Int -> onCancel() }
        builder.create().show()
    }

    private fun onCancel() {
        sendEventToRN("Cancel")
        val intent = Intent()
        setResult(ResponseCode.RESULT_CANCELED, intent)
        finish()
    }

    override fun onFilterSelected(photoFilter: PhotoFilter) {
        mPhotoEditor!!.setFilterEffect(photoFilter)
    }

    override fun onToolSelected(toolType: ToolType) {
        when (toolType) {
            ToolType.SHAPE -> {
                sendEventToRN("Draw")
                mPhotoEditor!!.setBrushDrawingMode(true)
                mShapeBuilder = ShapeBuilder()
                mPhotoEditor!!.setShape(mShapeBuilder)
                showBottomSheetDialogFragment(mShapeBSFragment)
            }

            ToolType.TEXT -> {
                sendEventToRN("Text")
                val textEditorDialogFragment = TextEditorDialogFragment.show(this)
                textEditorDialogFragment.setOnTextEditorListener { inputText: String?, colorCode: Int ->
                    val styleBuilder = TextStyleBuilder()
                    styleBuilder.withTextColor(colorCode)
                    mPhotoEditor!!.addText(inputText, styleBuilder)
                }
            }

            ToolType.ERASER -> {
                mPhotoEditor!!.brushEraser()
            }

            ToolType.FILTER -> {
                sendEventToRN("Filter")
                showFilter(true)
            }

            ToolType.STICKER -> {
                sendEventToRN("Stickers")
                showBottomSheetDialogFragment(mStickerFragment)
            }
        }
    }

    private fun showBottomSheetDialogFragment(fragment: BottomSheetDialogFragment?) {
        if (fragment == null || fragment.isAdded) {
            return
        }
        fragment.show(supportFragmentManager, fragment.tag)
    }

    private fun showFilter(show: Boolean) { }

    override fun onBackPressed() {
        onCancel()
    }

    companion object {
        private val TAG = PhotoEditorActivity::class.java.simpleName
        const val PINCH_TEXT_SCALABLE_INTENT_KEY = "PINCH_TEXT_SCALABLE"
        const val READ_WRITE_STORAGE = 52
        const val EVENT_EMITTER_DATA = "data"
        const val EVENT_EMITTER_EVENT = "EVENT_BARONA"
    }
}
