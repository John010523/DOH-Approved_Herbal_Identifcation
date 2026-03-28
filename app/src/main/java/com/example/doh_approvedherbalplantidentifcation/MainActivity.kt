package com.example.doh_approvedherbalplantidentifcation


import android.os.Looper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.animation.AnimatorInflater
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.Color
import android.graphics.Rect
import android.widget.AdapterView
import android.os.Handler
import android.view.View
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.widget.NestedScrollView
import com.example.doh_approvedherbalplantidentifcation.NextActivity.Companion.REQUEST_CAMERA_CAPTURE
import com.example.doh_approvedherbalplantidentifcation.ml.HerbalRecognationSemifi
import com.google.mlkit.vision.common.InputImage
import java.io.ByteArrayOutputStream
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
class MainActivity : AppCompatActivity() {

    private lateinit var continuebtn: Button
    private var isReady = false // Moved here for cleaner scope

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. MUST BE FIRST: Initialize splash screen before everything else
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 2. Set the condition to keep splash on screen
        splashScreen.setKeepOnScreenCondition { !isReady }

        // 3. Start your timer/loading task
        Handler(Looper.getMainLooper()).postDelayed({
            isReady = true
        }, 3000)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        continuebtn = findViewById(R.id.continuebtn)
        continuebtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CC47734D"))
        continuebtn.setOnClickListener {
            val intent = Intent(this, NextActivity::class.java)
            startActivity(intent)
        }
    }
}

// BaseActivity.kt
abstract class BaseActivity : AppCompatActivity() {

    @get:LayoutRes
    protected abstract val layoutResourceId: Int

    // Changed to nullable or checked initialization to prevent "not initialized" crashes
    var btnhome: ImageButton? = null
    var btnsave: ImageButton? = null
    var about: ImageButton? = null
    var scnherbs: ImageButton? = null
    var upload: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("DarkMode", false)

        // 2. Set the System Theme (for Status Bar/Icons)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        super.onCreate(savedInstanceState)
        setContentView(layoutResourceId)
    }
    fun applyBackgroundColor(view: View) {
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("Light Mode", false)

        if (isDarkMode) {
            view.setBackgroundColor(android.graphics.Color.parseColor("#121212"))
        } else {
            view.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
        }
    }

    protected fun setupNavigation() {
        // Safe lookups: if the ID doesn't exist in the current XML, it won't crash
        btnhome = findViewById(R.id.home)
        about = findViewById(R.id.aboutus)
        scnherbs = findViewById(R.id.scnherbs)
        upload = findViewById(R.id.upload)

        updateButtonSelection()

        // Only set listeners if the buttons actually exist in the layout
        btnhome?.setOnClickListener { navigateTo(NextActivity::class.java) }
        upload?.setOnClickListener { navigateTo(Classify::class.java) }
        scnherbs?.setOnClickListener {
            // FIX: If we are already in ScannerActivity, do nothing!
            if (this !is ScannerActivity) {
                navigateTo(ScannerActivity::class.java)
            }
        }
    }

    private fun navigateTo(cls: Class<*>) {
        if (this::class.java == cls) return

        val intent = Intent(this, cls)
        // This flag clears the stack; use it carefully
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun updateButtonSelection() {
        // Use ?.isSelected because these might be null in some layouts
        btnhome?.isSelected = (this is NextActivity)
        upload?.isSelected = (this is Classify)
        scnherbs?.isSelected = (this is ScannerActivity)
    }
}
//second activity layout
class NextActivity : BaseActivity() { // CHANGED THIS

    override val layoutResourceId: Int = R.layout.second_activity



    companion object {
        const val REQUEST_CAMERA_CAPTURE = 101
    }
    //private val handler = Handler(Looper.getMainLooper())
    private lateinit var frontimg: ImageView
    private lateinit var frame: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.second_activity)

        // 1. Initialize the global navigation from the parent
        setupNavigation()

        // 2. Setup specific logic for THIS activity
        frontimg = findViewById(R.id.front)
        frame = findViewById(R.id.fl)

        //startSingleImageFlip()

        val browsing = findViewById<TextView>(R.id.browsee)
        browsing.setOnClickListener {
            val intent = Intent(this, Category::class.java)
            startActivity(intent)
        }
        val sav = findViewById<TextView>(R.id.saved)
        sav.setOnClickListener {
            val intent = Intent(this, HerbActivity::class.java)
            startActivity(intent)
        }

    }
    override fun onResume() {
        super.onResume()
        // Call your permission check whenever the app comes to the foreground
        val second = findViewById<View>(R.id.second) // Ensure your XML root has this ID
        applyBackgroundColor(second)
        val h = findViewById<TextView>(R.id.heybud)

        val darkModeToggle = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.dark_mode_toggle)

        darkModeToggle.setOnCheckedChangeListener { _, isChecked ->
            val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("Dark Mode", isChecked)
            editor.apply()

            // 1. Manually change the colors WITHOUT recreate()
            if (isChecked) {
                second.setBackgroundColor(android.graphics.Color.parseColor("#121212"))
                darkModeToggle.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                h.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                darkModeToggle.setText("Light Mode")
            } else {
                second.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
                darkModeToggle.setTextColor(android.graphics.Color.parseColor("#121212"))
                h.setTextColor(android.graphics.Color.parseColor("#121212"))
                darkModeToggle.setText("Dark Mode")
            }
        }
    }


    /*private fun startSingleImageFlip() {
        val flipInterval = 3000L
        handler.post(object : Runnable {
            override fun run() {
                val flip = AnimatorInflater.loadAnimator(this@NextActivity, R.animator.front_animator)
                flip.setTarget(frontimg)
                flip.start()
                handler.postDelayed(this, flipInterval)
            }
        })*/
}

// Third activity for browsing
class Category : BaseActivity()    {

    override val layoutResourceId: Int = R.layout.category

    private lateinit var category_spinner: Spinner
    private lateinit var category_background: ImageView
    private lateinit var category_background2: ImageView
    private lateinit var herbalspinner: Spinner
    private lateinit var deseasespinner: Spinner
    private lateinit var herbalcategory: TextView
    private lateinit var deseasecategory: TextView
    private lateinit var adapterItem: ArrayAdapter<String>
    private lateinit var adapterItemHerb: ArrayAdapter<String> // Changed CharArray to String
    private lateinit var adapterItemDisease: ArrayAdapter<String>
    private lateinit var itemImage: ImageView
    private lateinit var itemDefinition: TextView
    private lateinit var defsv: ScrollView
    private lateinit var tit: TextView
    private lateinit var tit2: TextView
    private lateinit var tit3: TextView



    private lateinit var recommend: ImageView
    private lateinit var recommend2: ImageView
    private lateinit var recommend3: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.category)

        setupNavigation()

        //Dropdown categories
        val Choose_Category = arrayOf("Select A Category", "Herbal plant", "Disease")
        val Herb_Category = arrayOf("Select Herbs", "Akapulko","Ampalaya","Bawang","Bayabas","Lagundi", "Niyog-Niyogan","Pancit-Pancitan","Sambong","Tsaang-Gubat", "Yerba Buena") // Changed to Array<String>
        val Disease_Category = arrayOf("Select Disease", "Tooth Ache", "Stomach Ache", "Head Ache", "Back Ache", "Ear Ache")

        category_spinner = findViewById(R.id.category_spinner)
        category_background = findViewById(R.id.category_background)
        category_background2 = findViewById(R.id.category_background2)
        herbalspinner = findViewById(R.id.herbalspinner)
        deseasespinner = findViewById(R.id.deseasespinner)
        herbalcategory = findViewById(R.id.herbalcategory)
        deseasecategory = findViewById(R.id.deseasecategory)
        defsv = findViewById(R.id.svdef)
        tit = findViewById(R.id.title)
        tit2 = findViewById(R.id.title2)
        tit3 = findViewById(R.id.title3)
        recommend=findViewById(R.id.rec)
        recommend2=findViewById(R.id.rec2)
        recommend3=findViewById(R.id.rec3)
        itemImage = findViewById(R.id.item_image)
        itemDefinition = findViewById(R.id.item_definition)
        adapterItem = ArrayAdapter(this, android.R.layout.simple_spinner_item, Choose_Category)
        adapterItemHerb = ArrayAdapter(this, android.R.layout.simple_spinner_item, Herb_Category)
        adapterItemDisease = ArrayAdapter(this, android.R.layout.simple_spinner_item, Disease_Category)
        adapterItem.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapterItemHerb.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Added this line
        adapterItemDisease.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        category_spinner.adapter = adapterItem


        //Choices between Herbal and Deseases category
        category_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position)?.toString()?.trim()

                Toast.makeText(this@Category, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()

                if (selectedItem.equals("Herbal plant", ignoreCase = true)) {
                    herbalspinner.visibility = View.VISIBLE
                    herbalcategory.visibility = View.VISIBLE
                    itemImage.visibility = View.VISIBLE
                    itemDefinition.visibility = View.VISIBLE
                    defsv.visibility = View.VISIBLE
                    category_background.visibility = View.VISIBLE
                    category_background2.visibility = View.GONE
                    herbalcategory.text = ""
                    tit.visibility = View.INVISIBLE
                    tit2.visibility = View.INVISIBLE
                    tit3.visibility = View.INVISIBLE
                    herbalspinner.adapter = adapterItemHerb // Set adapter for herbalspinner
                    deseasespinner.visibility = View.GONE
                    deseasecategory.visibility = View.GONE
                } else if (selectedItem.equals("Disease", ignoreCase = true)) {
                    deseasespinner.visibility = View.VISIBLE
                    deseasecategory.visibility = View.VISIBLE
                    itemImage.visibility = View.VISIBLE
                    itemDefinition.visibility = View.VISIBLE
                    defsv.visibility = View.VISIBLE
                    tit.visibility = View.VISIBLE
                    tit2.visibility = View.VISIBLE
                    tit3.visibility = View.VISIBLE
                    category_background.visibility = View.GONE
                    category_background2.visibility = View.VISIBLE
                    deseasecategory.text = ""
                    herbalspinner.visibility = View.GONE
                    herbalcategory.visibility = View.GONE
                    deseasespinner.adapter = adapterItemDisease // Set adapter for deseasespinner
                } else {
                    // Hide both if "Select Category" or unknown
                    category_background.visibility = View.VISIBLE
                    category_background2.visibility = View.GONE
                    herbalspinner.visibility = View.GONE
                    herbalcategory.visibility = View.GONE
                    deseasespinner.visibility = View.GONE
                    deseasecategory.visibility = View.GONE
                    itemImage.visibility = View.GONE
                    itemDefinition.visibility = View.GONE
                    defsv.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                herbalspinner.visibility = View.GONE
                herbalcategory.visibility = View.GONE
                deseasespinner.visibility = View.GONE
                deseasecategory.visibility = View.GONE
                itemImage.visibility = View.GONE
                itemDefinition.visibility = View.GONE
                category_background.visibility = View.VISIBLE
            }
        }

        //Ini liwat Herbal tanan
        herbalspinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val herb = parent?.getItemAtPosition(position)?.toString()?.trim()
                herbalcategory.text = herb

                when (herb) {
                    "Akapulko" -> {
                        itemImage.setImageResource(R.drawable.akapulko)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.GONE
                        recommend2.visibility = View.GONE
                        recommend3.visibility = View.GONE
                        tit.visibility = View.GONE
                        tit2.visibility = View.GONE
                        tit3.visibility = View.GONE
                        itemDefinition.text = "Akapulko (Cassia alata) is known for its antifungal properties, used to treat ringworm, scabies, and eczema."
                        category_background.visibility = View.GONE
                    }
                    "Ampalaya" -> {
                        itemImage.setImageResource(R.drawable.ampalaya)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.GONE
                        recommend2.visibility = View.GONE
                        recommend3.visibility = View.GONE
                        tit.visibility = View.GONE
                        tit2.visibility = View.GONE
                        tit3.visibility = View.GONE
                        itemDefinition.text = "Ampalaya (Momordica charantia), also known as bitter melon, is used to help manage diabetes and lower blood sugar levels."
                        category_background.visibility = View.GONE
                    }
                    "Bawang" -> {
                        itemImage.setImageResource(R.drawable.bawang)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.GONE
                        recommend2.visibility = View.GONE
                        recommend3.visibility = View.GONE
                        tit.visibility = View.GONE
                        tit2.visibility = View.GONE
                        tit3.visibility = View.GONE
                        itemDefinition.text = "Bawang (Allium sativum), or garlic, is known for reducing cholesterol and helping control blood pressure."
                        category_background.visibility = View.GONE
                    }
                    "Bayabas" -> {
                        itemImage.setImageResource(R.drawable.bayabas)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.GONE
                        recommend2.visibility = View.GONE
                        recommend3.visibility = View.GONE
                        tit.visibility = View.GONE
                        tit2.visibility = View.GONE
                        tit3.visibility = View.GONE
                        itemDefinition.text = "Bayabas (Psidium guajava), or guava, is used as an antiseptic to disinfect wounds and as a mouthwash for tooth and gum infections."
                        category_background.visibility = View.GONE
                    }
                    "Lagundi" -> {
                        itemImage.setImageResource(R.drawable.lagundi)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.GONE
                        recommend2.visibility = View.GONE
                        recommend3.visibility = View.GONE
                        tit.visibility = View.GONE
                        tit2.visibility = View.GONE
                        tit3.visibility = View.GONE
                        itemDefinition.text = "Lagundi (Vitex negundo) is a shrub used to treat cough, asthma, and fever. Endorsed by the DOH for respiratory relief."
                        category_background.visibility = View.GONE
                    }
                    "Niyog-Niyogan" -> {
                        itemImage.setImageResource(R.drawable.niyog_niyogan)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.GONE
                        recommend2.visibility = View.GONE
                        recommend3.visibility = View.GONE
                        tit.visibility = View.GONE
                        tit2.visibility = View.GONE
                        tit3.visibility = View.GONE
                        itemDefinition.text = "Niyog-Niyogan (Combretum indicum) is a vine used as a dewormer and for treating headaches and skin ulcers."
                        category_background.visibility = View.GONE
                    }
                    "Pancit-Pancitan" -> {
                        itemImage.setImageResource(R.drawable.pancit_pancitan)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.GONE
                        recommend2.visibility = View.GONE
                        recommend3.visibility = View.GONE
                        tit.visibility = View.GONE
                        tit2.visibility = View.GONE
                        tit3.visibility = View.GONE
                        itemDefinition.text = "Pancit-Pancitan (Peperomia pellucida) is used for gout, arthritis, and skin disorders. It lowers uric acid levels."
                        category_background.visibility = View.GONE
                    }
                    "Sambong" -> {
                        itemImage.setImageResource(R.drawable.sambong)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.GONE
                        recommend2.visibility = View.GONE
                        recommend3.visibility = View.GONE
                        tit.visibility = View.GONE
                        tit2.visibility = View.GONE
                        tit3.visibility = View.GONE
                        itemDefinition.text = "Sambong (Blumea balsamifera) is a diuretic herb used for kidney stones, fever, and respiratory infections."
                        category_background.visibility = View.GONE
                    }
                    "Tsaang-Gubat" -> {
                        itemImage.setImageResource(R.drawable.tsaang_gubat)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.GONE
                        recommend2.visibility = View.GONE
                        recommend3.visibility = View.GONE
                        tit.visibility = View.GONE
                        tit2.visibility = View.GONE
                        tit3.visibility = View.GONE
                        itemDefinition.text = "Tsaang Gubat (Ehretia microphylla) is a wild tea used for stomach pain, diarrhea, and as a mouthwash."
                        category_background.visibility = View.GONE
                    }
                    "Yerba Buena" -> {
                        itemImage.setImageResource(R.drawable.yerba_buena)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.GONE
                        recommend2.visibility = View.GONE
                        recommend3.visibility = View.GONE
                        tit.visibility = View.GONE
                        tit2.visibility = View.GONE
                        tit3.visibility = View.GONE
                        itemDefinition.text = "Yerba Buena (Clinopodium douglasii) is a mint herb used for pain relief, digestive issues, and as a mouthwash."
                        category_background.visibility = View.GONE
                    }
                    else -> {
                        itemImage.setImageDrawable(null)
                        itemDefinition.text = ""
                        category_background.visibility = View.VISIBLE
                        defsv.visibility= View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                category_background.visibility = View.VISIBLE
            }
        }

        //Ini liwat Deseases tun
        deseasespinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val disease = parent?.getItemAtPosition(position)?.toString()?.trim()
                deseasecategory.text = disease

                when (disease) {
                    "Tooth Ache" -> {
                        itemImage.setImageResource(R.drawable.tooth_ache)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.VISIBLE
                        recommend2.visibility= View.VISIBLE
                        recommend3.visibility= View.VISIBLE
                        recommend.setImageResource(R.drawable.clove_oil)
                        recommend2.setImageResource(R.drawable.lagundi)
                        tit.setText("Clove Oil")
                        tit2.setText("Lagundi")
                        tit3.setText("Peppermint Leaf")
                        recommend3.setImageResource(R.drawable.peppermint_leaf)
                        itemDefinition.text = "Recommended Herbal Medicine: Clove Oil, Lagundi, Peppermint Leaf"
                        category_background2.visibility = View.GONE
                    }
                    "Stomach Ache" -> {
                        itemImage.setImageResource(R.drawable.stomach_ache)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.VISIBLE
                        recommend2.visibility= View.VISIBLE
                        recommend3.visibility= View.VISIBLE
                        recommend.setImageResource(R.drawable.ginger)
                        recommend2.setImageResource(R.drawable.chamomile)
                        recommend3.setImageResource(R.drawable.tsaang_gubat)
                        tit.setText("Ginger")
                        tit2.setText("Chamomile")
                        tit3.setText("Tsaang-Gubat")
                        itemDefinition.text = "Recommended Herbal Medicine: Ginger, Chamomile, Tsaang-Gubat"
                        category_background2.visibility = View.GONE
                    }
                    "Head Ache" -> {
                        itemImage.setImageResource(R.drawable.head_ache)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.VISIBLE
                        recommend2.visibility= View.VISIBLE
                        recommend3.visibility= View.VISIBLE
                        recommend.setImageResource(R.drawable.yerba_buena)
                        recommend2.setImageResource(R.drawable.feverfew)
                        recommend3.setImageResource(R.drawable.rosemary)
                        tit.setText("Yerba Buena")
                        tit2.setText("Feverfew")
                        tit3.setText("Rosemary")
                        itemDefinition.text = "Recommended Herbal Medicine: Yerba Buena, Feverfew, Rosemary"
                        category_background2.visibility = View.GONE
                    }
                    "Back Ache" -> {
                        itemImage.setImageResource(R.drawable.back_ache)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.VISIBLE
                        recommend2.visibility= View.VISIBLE
                        recommend3.visibility= View.VISIBLE
                        recommend.setImageResource(R.drawable.turmeric)
                        recommend2.setImageResource(R.drawable.sambong)
                        recommend3.setImageResource(R.drawable.wwb)
                        tit.setText("Turmeric")
                        tit2.setText("Sambong")
                        tit3.setText("White Willow Bark")
                        itemDefinition.text = "Recommended Herbal Medicine: Turmeric, Sambong, White Willow Bark"
                        category_background2.visibility = View.GONE
                    }
                    "Ear Ache" -> {
                        itemImage.setImageResource(R.drawable.ear_ache)
                        defsv.visibility= View.VISIBLE
                        recommend.visibility= View.VISIBLE
                        recommend2.visibility= View.VISIBLE
                        recommend3.visibility= View.VISIBLE
                        recommend.setImageResource(R.drawable.garlic_oil)
                        recommend2.setImageResource(R.drawable.mullein)
                        recommend3.setImageResource(R.drawable.chamomile)
                        tit.setText("Garlic Oil")
                        tit2.setText("Mullein")
                        tit3.setText("Chamomile")
                        itemDefinition.text = "Recommended Herbal Medicine: Garlic Oil, Mullein, Chamomile"
                        category_background2.visibility = View.GONE
                    }
                    else -> {
                        itemImage.setImageDrawable(null)
                        itemDefinition.text = ""
                        category_background2.visibility = View.VISIBLE
                        defsv.visibility= View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                itemImage.setImageDrawable(null)
                itemDefinition.text = ""
                category_background2.visibility = View.VISIBLE
            }
        }
    }

}

//Fourth Activity Ddi makita an saved predicted or classified photos
class HerbActivity : BaseActivity() {

    override val layoutResourceId: Int = R.layout.herblistview

    private lateinit var herblist: ListView
    private lateinit var sqLiteHelper: SQLiteHelper
    private var herbmod = mutableListOf<HerbModel>()

    private lateinit var adapter: HerbAdapter

    // Pagination variables
    private var currentOffset = 0
    private val pageSize = 15 // Increased for better initial fill
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.herblistview)

        setupNavigation()



        herblist = findViewById(R.id.herbListView)
        sqLiteHelper = SQLiteHelper(this)

        adapter = HerbAdapter(this, herbmod)
        herblist.adapter = adapter

        // 1. Initial Load
        loadNextBatch()


        // 2. Set Scroll Listener for "Infinite Scroll"
        herblist.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}

            override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                // If we are at the bottom and not currently loading, fetch more
                if (!isLoading && (firstVisibleItem + visibleItemCount >= totalItemCount) && totalItemCount > 0) {
                    loadNextBatch()
                }
            }
        })
    }

    private fun loadNextBatch() {
        isLoading = true
        try {
            val herbs = sqLiteHelper.getHerbs(pageSize, currentOffset)

            if (herbs.isNotEmpty()) {
                herbmod.addAll(herbs)
                adapter.notifyDataSetChanged()
                currentOffset += herbs.size
            }
        } catch (ex: Exception) {
            Log.e("HerbActivity", "Failed to load herbs", ex)
        } finally {
            isLoading = false
        }
    }

    override fun onResume() {
        super.onResume()
        applyBackgroundColor(findViewById(R.id.herb_list))
    }

}

//Fifth Activity Ini na part an pag classify ha image or pag predict
class Classify : BaseActivity() {

    override val layoutResourceId: Int = R.layout.captured_analysis

    companion object {
        const val SELECT_IMAGE = 101
    }

    private lateinit var classification: Button
    private lateinit var Imgcapture: ImageView
    private lateinit var Imglabel: TextView
    private lateinit var Up: Button
    private lateinit var Donebtn: Button
    private lateinit var Scanagain: Button
    private lateinit var Save: Button
    private lateinit var Desc: TextView
    private lateinit var Safe: TextView
    private lateinit var labelsafe: TextView
    private lateinit var labeldesc: TextView
    private val Request_Camera_Capture = REQUEST_CAMERA_CAPTURE
    private lateinit var sqLiteHelper: SQLiteHelper
    private lateinit var preplbl: TextView
    private lateinit var prepar: TextView
    private lateinit var name: TextView
    private var safety_warning: String = ""
    private var description: String = ""
    private var preparation: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.captured_analysis)

        setupNavigation()

        Imgcapture = findViewById(R.id.imagecapture)
        Imglabel = findViewById(R.id.label)
        Save = findViewById(R.id.savebutton)
        Up = findViewById(R.id.uploadbttn)
        Up.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4D47E9"))
        classification = findViewById(R.id.classify)
        classification.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4D47E9"))
        Scanagain = findViewById(R.id.scanagain)
        Scanagain.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4D47E9"))
        Donebtn = findViewById(R.id.donebtn)
        Desc = findViewById(R.id.descriptionlabel)
        labelsafe = findViewById(R.id.safe)
        labeldesc = findViewById(R.id.labeldes)
        Safe = findViewById(R.id.safety)
        preplbl = findViewById(R.id.preplabel)
        prepar = findViewById(R.id.prep)
        name = findViewById(R.id.name)

        val intent = intent




        val byteArray = intent.getByteArrayExtra("capturedImage")
        if (byteArray != null) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            Imgcapture.setImageBitmap(bitmap) // Set the bitmap to the ImageView
            Imgcapture.visibility = View.VISIBLE
            Log.d("ImageStatus", "Captured image displayed")
        }

        sqLiteHelper = SQLiteHelper(this)


        //  Select image from gallery
        Up.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, SELECT_IMAGE)
            Imglabel.setText(" ")
            labeldesc.setText(" ")
            Safe.setText(" ")
            preplbl.setText("")
        }

        //  Predict button
        classification.setOnClickListener {
            val drawable = Imgcapture.drawable
            if (drawable != null) {
                val bitmap = if (drawable is android.graphics.drawable.BitmapDrawable) {
                    drawable.bitmap
                } else {
                    // Handle other drawable types if necessary, e.g., create a new bitmap
                    val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.draw(canvas)
                    bmp
                }
                runPrediction(bitmap)
            } else { Toast.makeText(this, "No image to predict", Toast.LENGTH_SHORT).show()}
        }

        Donebtn.setOnClickListener {
            val intent = Intent(this, HerbActivity::class.java) // Replace MainActivity with your home class name
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        Scanagain.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            startActivity(intent)
        }

        Save.setOnClickListener {
            saveCurrentResult()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == SELECT_IMAGE && data != null) {
            val imageUri = data.data
            val imageBitmap = imageUri?.let {
                MediaStore.Images.Media.getBitmap(contentResolver, it)
            }

            if (imageBitmap != null) {
                Imgcapture.setImageBitmap(imageBitmap) // Set the bitmap to the ImageView
                Imgcapture.visibility = View.VISIBLE
                Log.d("ImageStatus", "Image selected and displayed")
            } else {
                Toast.makeText(this, "Failed to retrieve image", Toast.LENGTH_SHORT).show()
            }
        }
        if (resultCode == RESULT_OK && requestCode == Request_Camera_Capture && data != null) {
            val photo = data.extras?.get("data") as? Bitmap
            if (photo != null) {
                Imgcapture.setImageBitmap(photo) // Set the bitmap to the ImageView
                Imgcapture.visibility = View.VISIBLE
                Log.d("ImageStatus", "New captured image displayed for re-scan")
                Imglabel.text = "" // Clear previous label
            }
        }
    }

    //  Run Classification tima pag deploy san model
    private fun runPrediction(bitmap: Bitmap) {
        // Load labels from assets
        val labelList = assets.open("label.txt")
            .bufferedReader()
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val model = HerbalRecognationSemifi.newInstance(this)

        // Preprocess image
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(180, 180, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 1f))
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val inputFeature0 = TensorBuffer.createFixedSize(
            intArrayOf(1, 180, 180, 3), DataType.FLOAT32
        )
        inputFeature0.loadBuffer(tensorImage.buffer)

        //  Run model
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

        //  Apply softmax
        val probabilities = outputFeature0

        // 🔎 Log all class scores
        probabilities.forEachIndexed { idx, prob ->
            val name = labelList.getOrNull(idx) ?: "Class $idx"
            Log.d("MODEL_OUTPUT", "$name: ${"%.4f".format(prob)}")
        }

        //  Find top prediction
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] }
        val maxProb = maxIndex?.let { probabilities[it] } ?: 0f
        val confidenceThreshold = 0.3f  // Lower for testing
        val predictedLabel = if (maxProb >= confidenceThreshold &&
            maxIndex != null &&
            maxIndex in labelList.indices) {
            labelList[maxIndex]
        } else {
            "Invalid"
        }

        preparation = when (predictedLabel) {
            "Akapulko" -> "📝 Boil leaves and apply the decoction on affected skin areas twice daily for fungal infections."
            "Ampalaya" -> "📝 Boil the leaves or fruit and drink the decoction twice a day to help manage blood sugar."
            "Bawang" -> "📝 Crush cloves and eat raw or mix with food to help lower cholesterol and blood pressure."
            "Bayabas" -> "📝 Boil leaves for 15 minutes and use as a wash for wounds or as mouth rinse for gum infections."
            "Lagundi" -> "📝 Boil leaves and drink the decoction three times a day to relieve cough and fever."
            "Niyog_Niyogan" -> "📝 Roast and eat mature seeds (2 for children, 4 for adults) after meals as a dewormer."
            "Pansit_Pansitan" -> "📝 Eat fresh leaves as salad or boil and drink the decoction twice daily to relieve gout and arthritis."
            "Sambong" -> "📝 Boil leaves and drink the decoction two times a day for kidney stones or urinary issues."
            "Tsaang_Gubat" -> "📝 Boil leaves for 15 minutes, cool, and drink as tea to ease stomach pain or diarrhea."
            "Yerba_Buena" -> "📝 Boil leaves and drink as tea for headaches or stomach pain, or use as a mouthwash."
            else -> "📝 No preparation details available."
        }


        description = when (predictedLabel) {
            "Akapulko" -> "📖 Known for its antifungal properties — used to treat ringworm, scabies, and eczema."
            "Ampalaya" -> "📖 Helps manage diabetes and lower blood sugar levels."
            "Bawang" -> "📖 Known for reducing cholesterol and helping control blood pressure."
            "Bayabas" -> "📖 Acts as an antiseptic — used to clean wounds and as a mouthwash for tooth and gum infections."
            "Lagundi" -> "📖 Used to treat cough, asthma, and fever."
            "Niyog_Niyogan" -> "📖 Used as a dewormer and for treating headaches and skin ulcers."
            "Pansit_Pansitan" -> "📖 Helps with gout, arthritis, and skin disorders — lowers uric acid levels."
            "Sambong" -> "📖 A diuretic herb used for kidney stones, fever, and respiratory infections."
            "Tsaang_Gubat" -> "📖 Used for stomach pain, diarrhea, and as a mouthwash."
            "Yerba_Buena" -> "📖 Known for pain relief, digestive support, and oral care."
            else -> "📖 No description available."
        }


        safety_warning = when (predictedLabel) {
            "Akapulko" -> "⚠️ For external use only. Avoid contact with eyes and open wounds. Prolonged use may cause skin irritation."
            "Ampalaya" -> "⚠️ Not recommended for pregnant women or people taking diabetes medication without medical advice."
            "Bawang" -> "⚠️ May cause stomach irritation if taken in excess. Avoid before surgery due to blood-thinning effects."
            "Bayabas" -> "⚠️ Use guava leaf decoction in moderation; excessive use may lead to constipation or stomach upset."
            "Lagundi" -> "⚠️ Not advisable for pregnant or breastfeeding women. May cause dizziness in sensitive individuals."
            "Niyog_Niyogan" -> "⚠️ Seeds are toxic in large amounts. Always use in recommended doses only."
            "Pansit_Pansitan" -> "⚠️ May cause mild stomach discomfort. Avoid if pregnant or breastfeeding."
            "Sambong" -> "⚠️ Avoid long-term use without medical advice. Not suitable for people with kidney disease."
            "Tsaang_Gubat" -> "⚠️ Avoid excessive consumption as it may cause constipation. Not for long-term use."
            "Yerba_Buena" -> "⚠️ Avoid excessive intake; may cause drowsiness or stomach irritation."
            else -> "⚠️ No safety warning available. Consult a healthcare professional before use."
        }


        val label = if (maxProb >= confidenceThreshold &&
            maxIndex != null &&
            maxIndex in labelList.indices) {
            labelList[maxIndex]
        } else {
            "Invalid"
        }

        // change color and display label
            if (label == "Invalid!") {
                Imglabel.setTextColor(Color.RED)
                Imglabel.text = label
                Desc.visibility = View.GONE
                Safe.visibility = View.GONE
                labeldesc.visibility = View.GONE
                labelsafe.visibility = View.GONE
                preplbl.visibility = View.GONE
                prepar.visibility = View.GONE
                name.visibility = View.GONE

            } else {
                Imglabel.setTextColor(Color.BLACK)
                val confidenceText = "%.2f".format(maxProb * 100)
                Imglabel.text = "$label (Confidence: $confidenceText%)"
                Desc.visibility = View.VISIBLE
                Safe.visibility = View.VISIBLE
                labeldesc.visibility = View.VISIBLE
                labelsafe.visibility = View.VISIBLE
                prepar.visibility = View.VISIBLE
                preplbl.visibility = View.VISIBLE
                name.visibility = View.VISIBLE
                labeldesc.text = description
                Safe.text = safety_warning
                preplbl.text = preparation


            }

        model.close()
    }

    private fun saveCurrentResult() {
        val sqLiteHelper = SQLiteHelper(this)
        val drawable = Imgcapture.drawable
        val labelText = Imglabel.text.toString()

        if (drawable == null || labelText.isEmpty() || labelText == "Invalid" || labelText == "Invalid!") {
            Toast.makeText(this, "No valid classification result to save!", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Get the full bitmap from the drawable
        val fullBitmap = if (drawable is android.graphics.drawable.BitmapDrawable) {
            drawable.bitmap
        } else {
            val bmp = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bmp)
            drawable.draw(canvas)
            bmp
        }

        // 2. RESIZE THE BITMAP (Critical Fix)
        // Resize to a smaller max dimension (e.g., 300px or 500px) to keep file size small
        val maxDimension = 400
        val ratio = Math.min(
            maxDimension.toDouble() / fullBitmap.width,
            maxDimension.toDouble() / fullBitmap.height
        )
        val width = (ratio * fullBitmap.width).toInt()
        val height = (ratio * fullBitmap.height).toInt()
        val resizedBitmap = Bitmap.createScaledBitmap(fullBitmap, width, height, true)

        // 3. Compress to JPEG to further reduce size
        val stream = ByteArrayOutputStream()
        // Quality 80 is a good balance.
        // IMPORTANT: Use JPEG instead of PNG for photos (much smaller size)
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val imageBytes = stream.toByteArray()

        if (imageBytes.isEmpty()) {
            Log.e("SaveResult", "Image byte array is empty")
            Toast.makeText(this, "Image conversion failed!", Toast.LENGTH_SHORT).show()
            return
        }

        // Check size immediately
        if (imageBytes.size > 1000000) { // If larger than 1MB
            Toast.makeText(this, "Image is too large to save!", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse label and confidence
        val parts = labelText.split("(")
        val herbalname = parts[0].trim()
        val confidenceString = if (parts.size > 1) parts[1].replace(")", "").trim() else ""

        val herballevel = try {
            // Handle cases like "Confidence: 95.50%"
            val cleanString = confidenceString.replace("Confidence:", "").replace("%", "").trim()
            cleanString.toFloat() / 100.0f
        } catch (e: Exception) {
            Log.e("SaveResult", "Could not parse confidence: $confidenceString", e)
            0.0f
        }

        // Use the class-level variables you already populated in runPrediction
        val herbaldescription = this.description
        val herbalsafetywarn = this.safety_warning

        // Attempt to insert
        val success = sqLiteHelper.insertHerb(
            herbalname,
            herballevel,
            herbaldescription,
            herbalsafetywarn,
            imageBytes
        )

        if (success) {
            Toast.makeText(this, "Saved to History!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Save failed!", Toast.LENGTH_SHORT).show()
        }
    }


}
// Real-Time Scanner
// Change AppCompatActivity to BaseActivity
class ScannerActivity : BaseActivity() {

    private lateinit var Scan: ImageButton
    private lateinit var Prev: PreviewView
    private lateinit var Txtres: TextView
    private lateinit var view: View
    private lateinit var herbalModel: HerbalRecognationSemifi
    private lateinit var objectDetector: com.google.mlkit.vision.objects.ObjectDetector
    private var herbalLabels: List<String> = emptyList()

    // Added your requested override variable
    override val layoutResourceId: Int = R.layout.viewfinder
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.viewfinder)

        setupNavigation()

        setupHerbalModel()
        loadLabels()

        Scan = findViewById<ImageButton>(R.id.Scan)
        Prev = findViewById<PreviewView>(R.id.Prev)
        Txtres = findViewById<TextView>(R.id.Txtres)
        view = findViewById<View>(R.id.targetBox)

        Scan.setOnClickListener {
            checkCameraPermission()


        }
    }

    private fun setupHerbalModel() {
        try {
            herbalModel = HerbalRecognationSemifi.newInstance(this)
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .build()
            objectDetector = com.google.mlkit.vision.objects.ObjectDetection.getClient(options)
        } catch (e: Exception) {
            Log.e("Scanner", "Initialization failed: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Call your permission check whenever the app comes to the foreground
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    private fun startCamera() {
        Scan.visibility = View.GONE
        Prev.visibility = View.VISIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(Prev.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                processImageProxy(imageProxy)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e("Scanner", "Use case binding failed", exc)
            }
            setupNavigation()
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        objectDetector.process(image)
            .addOnSuccessListener { objects ->
                updateTargetBoxToCenter()

                if (objects.isEmpty()) {
                    runOnUiThread {
                        Txtres.text = "Align plant in box..."
                        Txtres.setTextColor(Color.WHITE)
                        view.alpha = 0.5f
                    }
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                for (detectedObject in objects) {
                    val boundingBox = detectedObject.boundingBox

                    val isRotated = rotation == 90 || rotation == 270
                    val frameWidth = if (isRotated) imageProxy.height.toFloat() else imageProxy.width.toFloat()
                    val frameHeight = if (isRotated) imageProxy.width.toFloat() else imageProxy.height.toFloat()

                    val scaleX = frameWidth / Prev.width.toFloat()
                    val scaleY = frameHeight / Prev.height.toFloat()

                    val targetBoxInImage = Rect(
                        (view.left * scaleX).toInt(),
                        (view.top * scaleY).toInt(),
                        (view.right * scaleX).toInt(),
                        (view.bottom * scaleY).toInt()
                    )

                    if (targetBoxInImage.contains(boundingBox.centerX(), boundingBox.centerY())) {
                        runOnUiThread { view.alpha = 1.0f }

                        val fullBitmap = Prev.bitmap
                        if (fullBitmap != null) {
                            val bitmapScaleX = fullBitmap.width.toFloat() / frameWidth
                            val bitmapScaleY = fullBitmap.height.toFloat() / frameHeight

                            val scaledRect = Rect(
                                (boundingBox.left * bitmapScaleX).toInt(),
                                (boundingBox.top * bitmapScaleY).toInt(),
                                (boundingBox.right * bitmapScaleX).toInt(),
                                (boundingBox.bottom * bitmapScaleY).toInt()
                            )

                            val croppedBitmap = cropBitmap(fullBitmap, scaledRect)
                            runHerbalModel(croppedBitmap)
                        }
                    } else {
                        runOnUiThread {
                            Txtres.text = "Move plant into the scanner"
                            Txtres.setTextColor(Color.YELLOW)
                            view.alpha = 0.3f
                        }
                    }
                }
            }
            .addOnFailureListener { imageProxy.close() }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun updateTargetBoxToCenter() {
        runOnUiThread {
            val size = (Prev.width * 0.75).toInt()

            if (view.layoutParams.width != size) {
                val params = view.layoutParams
                params.width = size
                params.height = size
                view.layoutParams = params
                view.setBackgroundResource(R.drawable.scanner_design)
            }

            val centerX = (Prev.width / 2f) - (size / 2f)
            val centerY = (Prev.height / 2f) - (size / 2f)

            view.x = Prev.left + centerX
            view.y = Prev.top + centerY

            view.visibility = View.VISIBLE
        }
    }

    fun cropBitmap(source: Bitmap, rect: Rect): Bitmap {
        val left = rect.left.coerceIn(0, source.width - 1)
        val top = rect.top.coerceIn(0, source.height - 1)
        val width = rect.width().coerceAtMost(source.width - left)
        val height = rect.height().coerceAtMost(source.height - top)

        return if (width > 10 && height > 10) {
            Bitmap.createBitmap(source, left, top, width, height)
        } else source
    }

    private fun loadLabels() {
        try {
            val inputStream = assets.open("label.txt")
            herbalLabels = inputStream.bufferedReader().readLines().filter { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e("Scanner", "Error: ${e.message}")
        }
    }

    private fun runHerbalModel(bitmap: Bitmap) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 180, 180, true)
        val inputFeature0 = org.tensorflow.lite.support.tensorbuffer.TensorBuffer.createFixedSize(intArrayOf(1, 180, 180, 3), org.tensorflow.lite.DataType.FLOAT32)
        val byteBuffer = java.nio.ByteBuffer.allocateDirect(4 * 180 * 180 * 3).order(java.nio.ByteOrder.nativeOrder())

        val intValues = IntArray(180 * 180)
        scaledBitmap.getPixels(intValues, 0, 180, 0, 0, 180, 180)

        for (pixel in intValues) {
            byteBuffer.putFloat((pixel shr 16 and 0xFF).toFloat())
            byteBuffer.putFloat((pixel shr 8 and 0xFF).toFloat())
            byteBuffer.putFloat((pixel and 0xFF).toFloat())
        }
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = herbalModel.process(inputFeature0)
        val scores = outputs.outputFeature0AsTensorBuffer.floatArray
        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: -1

        runOnUiThread {
            if (maxIndex != -1 && maxIndex < herbalLabels.size) {
                val confidence = scores[maxIndex]
                val detectedLabel = herbalLabels[maxIndex]

                if (confidence > 0.65f) {
                    if (detectedLabel.contains("Invalid!", ignoreCase = true)) {
                        Txtres.text = "Invalid: Not a recognized plant"
                        Txtres.setTextColor(Color.RED)
                    } else {
                        Txtres.text = "$detectedLabel (${(confidence * 100).toInt()}%)"
                        Txtres.setTextColor(Color.GREEN)
                    }
                } else {
                    Txtres.text = "Analyzing..."
                    Txtres.setTextColor(Color.CYAN)
                }
            }
        }
    }
}