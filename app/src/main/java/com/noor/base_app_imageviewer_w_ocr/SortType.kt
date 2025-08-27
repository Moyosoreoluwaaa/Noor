//package com.noor.base_app_1.data
//
//import android.Manifest
//import android.app.Activity
//import android.content.ContentUris
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.provider.MediaStore
//import android.view.GestureDetector
//import android.view.LayoutInflater
//import android.view.MotionEvent
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.RadioGroup
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.core.content.ContextCompat.startActivity
//import androidx.core.view.WindowCompat
//import androidx.core.view.WindowInsetsCompat
//import androidx.core.view.WindowInsetsControllerCompat
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewModelScope
//import androidx.recyclerview.widget.GridLayoutManager
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import androidx.viewpager2.widget.ViewPager2
//import com.bumptech.glide.Glide
//import com.google.android.material.bottomsheet.BottomSheetDialog
//import com.noor.R
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.io.File
//import kotlin.jvm.java
//
//// Enums
//enum class SortType {
//    DATE_ASC,
//    DATE_DESC,
//    SIZE_ASC,
//    SIZE_DESC
//}
//
//enum class FilterType {
//    ALL,
//    BY_FOLDER
//}
//
//enum class ViewType {
//    GRID,
//    FULLSCREEN
//}
//
//// Data Classes
//data class ImageItem(
//    val id: Long,
//    val path: String,
//    val name: String,
//    val size: Long,
//    val dateModified: Long,
//    val folderName: String,
//    val folderPath: String,
//    val uri: Uri
//)
//
//data class Folder(
//    val name: String,
//    val path: String,
//    val imageCount: Int,
//    val coverImage: ImageItem?
//)
//
//data class ViewState(
//    val images: List<ImageItem> = emptyList(),
//    val folders: List<Folder> = emptyList(),
//    val currentFolder: Folder? = null,
//    val sortType: SortType = SortType.DATE_DESC,
//    val filterType: FilterType = FilterType.ALL,
//    val isLoading: Boolean = false,
//    val error: String? = null
//)
//
//// Permissions (Android 14 compatible)
//class PermissionManager(private val activity: Activity) {
//
//    companion object {
//        private const val PERMISSION_REQUEST_CODE = 100
//    }
//
//    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
//    } else {
//        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
//    }
//
//    fun hasPermissions(): Boolean {
//        return requiredPermissions.all { permission ->
//            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
//        }
//    }
//
//    fun requestPermissions() {
//        ActivityCompat.requestPermissions(activity, requiredPermissions, PERMISSION_REQUEST_CODE)
//    }
//
//    fun handlePermissionResult(requestCode: Int, grantResults: IntArray): Boolean {
//        return if (requestCode == PERMISSION_REQUEST_CODE) {
//            grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
//        } else {
//            false
//        }
//    }
//}
//
//// Image Grid Adapter
//class ImageGridAdapter(
//    private val onImageClick: (ImageItem, Int) -> Unit
//) : RecyclerView.Adapter<ImageGridAdapter.ImageViewHolder>() {
//
//    private var images = listOf<ImageItem>()
//
//    fun updateImages(newImages: List<ImageItem>) {
//        images = newImages
//        notifyDataSetChanged()
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
//        val binding = ItemImageGridBinding.inflate(
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//        return ImageViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
//        holder.bind(images[position], position)
//    }
//
//    override fun getItemCount(): Int = images.size
//
//    inner class ImageViewHolder(
//        private val binding: ItemImageGridBinding
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(imageItem: ImageItem, position: Int) {
//            Glide.with(binding.imageView)
//                .load(imageItem.uri)
//                .centerCrop()
//                .placeholder(R.drawable.placeholder_image)
//                .into(binding.imageView)
//
//            binding.root.setOnClickListener {
//                onImageClick(imageItem, position)
//            }
//        }
//    }
//}
//
//// Folder Adapter
//class FolderAdapter(
//    private val onFolderClick: (Folder) -> Unit
//) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {
//
//    private var folders = listOf<Folder>()
//
//    fun updateFolders(newFolders: List<Folder>) {
//        folders = newFolders
//        notifyDataSetChanged()
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
//        val binding = ItemFolderBinding.inflate(
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//        return FolderViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
//        holder.bind(folders[position])
//    }
//
//    override fun getItemCount(): Int = folders.size
//
//    inner class FolderViewHolder(
//        private val binding: ItemFolderBinding
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(folder: Folder) {
//            binding.textFolderName.text = folder.name
//            binding.textImageCount.text = "${folder.imageCount} images"
//
//            folder.coverImage?.let { coverImage ->
//                Glide.with(binding.imageCover)
//                    .load(coverImage.uri)
//                    .centerCrop()
//                    .placeholder(R.drawable.placeholder_image)
//                    .into(binding.imageCover)
//            }
//
//            binding.root.setOnClickListener {
//                onFolderClick(folder)
//            }
//        }
//    }
//}
//
//// Fullscreen Activity
//class FullscreenActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityFullscreenBinding
//    private lateinit var viewModel: NoorViewModel
//    private lateinit var imageAdapter: FullscreenPagerAdapter
//
//    private var isTopBarVisible = true
//    private var currentPosition = 0
//    private var images = listOf<ImageItem>()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityFullscreenBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Hide system bars
//        hideSystemUI()
//
//        setupViewModel()
//        setupUI()
//
//        currentPosition = intent.getIntExtra("image_position", 0)
//        val folderName = intent.getStringExtra("folder_name")
//
//        // Load images for the specific folder or all images
//        viewModel.viewState.observe(this) { state ->
//            images = if (folderName != null) {
//                viewModel.getImagesForFolder(folderName)
//            } else {
//                state.images
//            }
//
//            imageAdapter.updateImages(images)
//            binding.viewPager.setCurrentItem(currentPosition, false)
//            updateTopBar()
//        }
//    }
//
//    private fun setupViewModel() {
//        val repository = ImageRepository(this)
//        viewModel = ViewModelProvider(
//            this,
//            object : ViewModelProvider.Factory {
//                override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                    @Suppress("UNCHECKED_CAST")
//                    return NoorViewModel(repository) as T
//                }
//            }
//        )[NoorViewModel::class.java]
//    }
//
//    private fun setupUI() {
//        imageAdapter = FullscreenPagerAdapter { toggleTopBar() }
//        binding.viewPager.adapter = imageAdapter
//
//        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                currentPosition = position
//                updateTopBar()
//            }
//        })
//
//        binding.toolbar.setNavigationOnClickListener {
//            finish()
//        }
//
//        // Initially hide top bar after a delay
//        binding.root.postDelayed({
//            hideTopBar()
//        }, 2000)
//    }
//
//    private fun updateTopBar() {
//        if (images.isNotEmpty() && currentPosition < images.size) {
//            binding.toolbar.title = images[currentPosition].name
//        }
//    }
//
//    private fun toggleTopBar() {
//        if (isTopBarVisible) {
//            hideTopBar()
//        } else {
//            showTopBar()
//        }
//    }
//
//    private fun showTopBar() {
//        isTopBarVisible = true
//        binding.topBar.animate()
//            .translationY(0f)
//            .alpha(1f)
//            .setDuration(300)
//            .start()
//    }
//
//    private fun hideTopBar() {
//        isTopBarVisible = false
//        binding.topBar.animate()
//            .translationY(-binding.topBar.height.toFloat())
//            .alpha(0f)
//            .setDuration(300)
//            .start()
//    }
//
//    private fun hideSystemUI() {
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//        WindowInsetsControllerCompat(window, binding.root).let { controller ->
//            controller.hide(WindowInsetsCompat.Type.systemBars())
//            controller.systemBarsBehavior =
//                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//        }
//    }
//}
//
//// Fullscreen Pager Adapter
//class FullscreenPagerAdapter(
//    private val onImageTap: () -> Unit
//) : RecyclerView.Adapter<FullscreenPagerAdapter.ImageViewHolder>() {
//
//    private var images = listOf<ImageItem>()
//
//    fun updateImages(newImages: List<ImageItem>) {
//        images = newImages
//        notifyDataSetChanged()
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
//        val binding = ItemFullscreenImageBinding.inflate(
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//        return ImageViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
//        holder.bind(images[position])
//    }
//
//    override fun getItemCount(): Int = images.size
//
//    inner class ImageViewHolder(
//        private val binding: ItemFullscreenImageBinding
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(imageItem: ImageItem) {
//            binding.photoView.apply {
//                // Configure PhotoView
//                minimumScale = 0.5f
//                mediumScale = 1.0f
//                maximumScale = 3.0f
//
//                setOnPhotoTapListener { _, _, _ ->
//                    onImageTap()
//                }
//
//                setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
//                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
//                        onImageTap()
//                        return true
//                    }
//
//                    override fun onDoubleTap(e: MotionEvent): Boolean {
//                        if (scale > minimumScale) {
//                            setScale(minimumScale, true)
//                        } else {
//                            setScale(mediumScale, e.x, e.y, true)
//                        }
//                        return true
//                    }
//
//                    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
//                        return false
//                    }
//                })
//            }
//
//            Glide.with(binding.photoView)
//                .load(imageItem.uri)
//                .fitCenter()
//                .placeholder(R.drawable.placeholder_image)
//                .into(binding.photoView)
//        }
//    }
//}
//
//// Image Repository
//class ImageRepository(private val context: Context) {
//
//    suspend fun loadImages(): List<ImageItem> = withContext(Dispatchers.IO) {
//        val images = mutableListOf<ImageItem>()
//        val projection = arrayOf(
//            MediaStore.Images.Media._ID,
//            MediaStore.Images.Media.DISPLAY_NAME,
//            MediaStore.Images.Media.DATA,
//            MediaStore.Images.Media.SIZE,
//            MediaStore.Images.Media.DATE_MODIFIED,
//            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
//        )
//
//        val cursor = context.contentResolver.query(
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            projection,
//            null,
//            null,
//            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
//        )
//
//        cursor?.use {
//            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
//            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
//            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
//            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
//            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
//            val bucketColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
//
//            while (it.moveToNext()) {
//                val id = it.getLong(idColumn)
//                val name = it.getString(nameColumn)
//                val path = it.getString(dataColumn)
//                val size = it.getLong(sizeColumn)
//                val dateModified = it.getLong(dateColumn)
//                val bucketName = it.getString(bucketColumn) ?: "Unknown"
//
//                val uri = ContentUris.withAppendedId(
//                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                    id
//                )
//
//                images.add(
//                    ImageItem(
//                        id = id,
//                        path = path,
//                        name = name,
//                        size = size,
//                        dateModified = dateModified * 1000, // Convert to milliseconds
//                        folderName = bucketName,
//                        folderPath = File(path).parent ?: "",
//                        uri = uri
//                    )
//                )
//            }
//        }
//
//        images
//    }
//
//    suspend fun loadFolders(): List<Folder> = withContext(Dispatchers.IO) {
//        val images = loadImages()
//        val folderMap = images.groupBy { it.folderName }
//
//        folderMap.map { (folderName, folderImages) ->
//            Folder(
//                name = folderName,
//                path = folderImages.first().folderPath,
//                imageCount = folderImages.size,
//                coverImage = folderImages.first()
//            )
//        }.sortedByDescending { it.imageCount }
//    }
//}
//
//// ViewModel
//class NoorViewModel(private val repository: ImageRepository) : ViewModel() {
//
//    private val _viewState = MutableLiveData(ViewState())
//    val viewState: LiveData<ViewState> = _viewState
//
//    init {
//        loadData()
//    }
//
//    fun loadData() {
//        viewModelScope.launch {
//            _viewState.value = _viewState.value?.copy(isLoading = true)
//
//            try {
//                val images = repository.loadImages()
//                val folders = repository.loadFolders()
//
//                _viewState.value = _viewState.value?.copy(
//                    images = images,
//                    folders = folders,
//                    isLoading = false,
//                    error = null
//                )
//            } catch (e: Exception) {
//                _viewState.value = _viewState.value?.copy(
//                    isLoading = false,
//                    error = e.message
//                )
//            }
//        }
//    }
//
//    fun setSortType(sortType: SortType) {
//        val currentState = _viewState.value ?: return
//        val sortedImages = when (sortType) {
//            SortType.DATE_ASC -> currentState.images.sortedBy { it.dateModified }
//            SortType.DATE_DESC -> currentState.images.sortedByDescending { it.dateModified }
//            SortType.SIZE_ASC -> currentState.images.sortedBy { it.size }
//            SortType.SIZE_DESC -> currentState.images.sortedByDescending { it.size }
//        }
//
//        _viewState.value = currentState.copy(
//            images = sortedImages,
//            sortType = sortType
//        )
//    }
//
//    fun setCurrentFolder(folder: Folder?) {
//        val currentState = _viewState.value ?: return
//        val filteredImages = if (folder != null) {
//            currentState.images.filter { it.folderName == folder.name }
//        } else {
//            repository.loadImages() // Reload all images
//        }
//
//        _viewState.value = currentState.copy(
//            currentFolder = folder,
//            images = filteredImages
//        )
//    }
//
//    fun getImagesForFolder(folderName: String): List<ImageItem> {
//        return _viewState.value?.images?.filter { it.folderName == folderName } ?: emptyList()
//    }
//}
//
//// Album Screen Activity
//class AlbumActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityAlbumBinding
//    private lateinit var viewModel: NoorViewModel
//    private lateinit var permissionManager: PermissionManager
//    private lateinit var imageAdapter: ImageGridAdapter
//    private lateinit var folderAdapter: FolderAdapter
//
//    private var isGridView = true
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityAlbumBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        permissionManager = PermissionManager(this)
//        setupViewModel()
//        setupUI()
//
//        if (permissionManager.hasPermissions()) {
//            viewModel.loadData()
//        } else {
//            showPermissionBottomSheet()
//        }
//    }
//
//    private fun setupViewModel() {
//        val repository = ImageRepository(this)
//        viewModel = ViewModelProvider(
//            this,
//            object : ViewModelProvider.Factory {
//                override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                    @Suppress("UNCHECKED_CAST")
//                    return NoorViewModel(repository) as T
//                }
//            }
//        )[NoorViewModel::class.java]
//
//        viewModel.viewState.observe(this) { state ->
//            updateUI(state)
//        }
//    }
//
//    private fun setupUI() {
//        // Setup RecyclerView
//        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
//
//        imageAdapter = ImageGridAdapter { imageItem, position ->
//            openFullscreen(imageItem, position)
//        }
//
//        folderAdapter = FolderAdapter { folder ->
//            viewModel.setCurrentFolder(folder)
//            isGridView = true
//            binding.recyclerView.adapter = imageAdapter
//        }
//
//        binding.recyclerView.adapter = imageAdapter
//
//        // Setup toolbar
//        setupToolbar()
//    }
//
//    private fun setupToolbar() {
//        binding.toolbar.setOnMenuItemClickListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.action_sort -> {
//                    showSortBottomSheet()
//                    true
//                }
//                R.id.action_folder_view -> {
//                    toggleViewMode()
//                    true
//                }
//                else -> false
//            }
//        }
//
//        binding.toolbar.setNavigationOnClickListener {
//            if (viewModel.viewState.value?.currentFolder != null) {
//                viewModel.setCurrentFolder(null)
//                binding.recyclerView.adapter = folderAdapter
//                isGridView = false
//            } else {
//                finish()
//            }
//        }
//    }
//
//    private fun toggleViewMode() {
//        isGridView = !isGridView
//        if (isGridView) {
//            binding.recyclerView.adapter = imageAdapter
//            binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
//        } else {
//            binding.recyclerView.adapter = folderAdapter
//            binding.recyclerView.layoutManager = LinearLayoutManager(this)
//        }
//    }
//
//    private fun updateUI(state: ViewState) {
//        if (state.isLoading) {
//            binding.progressBar.visibility = View.VISIBLE
//        } else {
//            binding.progressBar.visibility = View.GONE
//        }
//
//        if (state.error != null) {
//            Toast.makeText(this, state.error, Toast.LENGTH_LONG).show()
//        }
//
//        // Update adapter data
//        if (isGridView) {
//            imageAdapter.updateImages(state.images)
//        } else {
//            folderAdapter.updateFolders(state.folders)
//        }
//
//        // Update toolbar title
//        val title = state.currentFolder?.name ?: "Noor"
//        binding.toolbar.title = title
//
//        // Show back arrow if in folder view
//        binding.toolbar.navigationIcon = if (state.currentFolder != null) {
//            ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
//        } else null
//    }
//
//    private fun openFullscreen(imageItem: ImageItem, position: Int) {
//        val intent = Intent(this, FullscreenActivity::class.java).apply {
//            putExtra("image_position", position)
//            putExtra("folder_name", viewModel.viewState.value?.currentFolder?.name)
//        }
//        startActivity(intent)
//    }
//
//    private fun showPermissionBottomSheet() {
//        val bottomSheet = BottomSheetDialog(this)
//        val view = layoutInflater.inflate(R.layout.bottom_sheet_permission, null)
//
//        view.findViewById<Button>(R.id.btn_grant_permission).setOnClickListener {
//            permissionManager.requestPermissions()
//            bottomSheet.dismiss()
//        }
//
//        bottomSheet.setContentView(view)
//        bottomSheet.show()
//    }
//
//    private fun showSortBottomSheet() {
//        val bottomSheet = BottomSheetDialog(this)
//        val view = layoutInflater.inflate(R.layout.bottom_sheet_sort, null)
//
//        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_sort)
//
//        // Set current selection
//        val currentSort = viewModel.viewState.value?.sortType ?: SortType.DATE_DESC
//        val checkedId = when (currentSort) {
//            SortType.DATE_ASC -> R.id.radio_date_asc
//            SortType.DATE_DESC -> R.id.radio_date_desc
//            SortType.SIZE_ASC -> R.id.radio_size_asc
//            SortType.SIZE_DESC -> R.id.radio_size_desc
//        }
//        radioGroup.check(checkedId)
//
//        radioGroup.setOnCheckedChangeListener { _, checkedId ->
//            val sortType = when (checkedId) {
//                R.id.radio_date_asc -> SortType.DATE_ASC
//                R.id.radio_date_desc -> SortType.DATE_DESC
//                R.id.radio_size_asc -> SortType.SIZE_ASC
//                R.id.radio_size_desc -> SortType.SIZE_DESC
//                else -> SortType.DATE_DESC
//            }
//            viewModel.setSortType(sortType)
//            bottomSheet.dismiss()
//        }
//
//        bottomSheet.setContentView(view)
//        bottomSheet.show()
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (permissionManager.handlePermissionResult(requestCode, grantResults)) {
//            viewModel.loadData()
//        } else {
//            Toast.makeText(this, "Permission required to access images", Toast.LENGTH_LONG).show()
//            finish()
//        }
//    }
//}