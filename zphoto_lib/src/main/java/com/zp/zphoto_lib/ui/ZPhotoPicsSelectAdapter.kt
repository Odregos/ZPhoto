package com.zp.zphoto_lib.ui

import android.content.Context
import android.support.v4.util.ArrayMap
import android.util.SparseBooleanArray
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.zp.zphoto_lib.R
import com.zp.zphoto_lib.common.BaseZPhotoAdapter
import com.zp.zphoto_lib.common.BaseZPhotoHolder
import com.zp.zphoto_lib.common.ZPhotoHelp
import com.zp.zphoto_lib.content.*
import com.zp.zphoto_lib.util.ZLog
import com.zp.zphoto_lib.util.ZPhotoUtil
import com.zp.zphoto_lib.util.ZToaster
import java.io.File

class ZPhotoPicsSelectAdapter(context: Context, layoutID: Int, spanCount: Int) : BaseZPhotoAdapter<ZPhotoDetail>(context, layoutID) {

    // 保存已选择的图片数据
    private var selectedMap = ArrayMap<String, ZPhotoDetail>()
    // 记录当前box选中的下标
    private var selectedArray = SparseBooleanArray()

    var zPhotoSelectListener: ZPhotoSelectListener? = null

    private val config by lazy {
        ZPhotoHelp.getInstance().getConfiguration()
    }

    private var wh = 0

    init {
        wh = (context.getDisplay()[0] - dip2px(1f) * 2 * spanCount) / spanCount
    }

    override fun bindView(holder: BaseZPhotoHolder, item: ZPhotoDetail, position: Int) {
        holder.apply {
            val durationTxt = getView<TextView>(R.id.item_zphoto_select_videoDurationiTxt)
            val box = getView<CheckBox>(R.id.item_zphoto_select_box)
            box.isChecked = selectedArray[position]

            if (config.allSelect) { // 能够同时选择图片和视频
                box.visibility = View.VISIBLE
            } else {
                box.visibility = if (item.isVideo) View.GONE else View.VISIBLE
            }

            val pic = getView<ImageView>(R.id.item_zphoto_select_pic)
            if (item.isVideo) {
                durationTxt.visibility = View.VISIBLE
                durationTxt.text = ZPhotoUtil.videoDurationFormat(item.duration)
            } else {
                durationTxt.visibility = View.GONE
            }

            pic.apply {
                layoutParams = FrameLayout.LayoutParams(wh, wh).apply {
                    gravity = Gravity.CENTER
                }
                ZPhotoHelp.getInstance().getImageLoaderListener().loadImg(this, File(item.path))
            }

            box.setOnClickListener {
                boxClick(item, position, box)
            }
        }
    }

    override fun setDatas(list: List<ZPhotoDetail>?) {
        if (!list.isNullOrEmpty()) {
            selectedArray.clear()
            list.indices.forEach {
                if (selectedMap.isNullOrEmpty()) {
                    selectedArray.put(it, false)
                } else {
                    selectedArray.put(it, selectedMap.containsValue(list[it]))
                }
            }
        }
        super.setDatas(list)
    }

    fun getSelectedData() = ArrayList<ZPhotoDetail>().apply {
        for ((_, v) in selectedMap) {
            add(v)
        }
    }

    /**
     * 点击逻辑
     */
    private fun boxClick(item: ZPhotoDetail, position: Int, box: CheckBox) {
        ZLog.i("选择了 ${item.path}  大小：${item.size}M")
        if (selectedArray[position]) { // 选中-->>不选中
            if (selectedMap.contains(item.path)) { // 包含删除
                selectedMap.remove(item.path)
                selectedArray.put(position, !selectedArray[position])
                zPhotoSelectListener?.selected(selectedMap.size)
            } else {
                ZLog.e("当前取消选中的不在map集合里面")
            }
        } else { // 不选中-->>选中
            val counts = getSelectPicOrVideoCount()
            if (item.isVideo) {
                // 判断视频大小
                if (item.size > config.maxVideoSize) {
                    ZToaster.makeTextS("单个视频最大可选取的大小：${config.maxVideoSize}M")
                    box.isChecked = false
                    return
                }
                val videoCount = counts.second
                if (videoCount >= config.maxVideoSelect) {
                    ZToaster.makeTextS("视频最多可选取的数量：${config.maxVideoSelect}")
                    box.isChecked = false
                } else {
                    selectedMap[item.path] = item
                    selectedArray.put(position, !selectedArray[position])
                    zPhotoSelectListener?.selected(selectedMap.size)
                }
            } else {
                // 判断图片大小
                if (item.size > config.maxPicSize) {
                    ZToaster.makeTextS("单张图片最大可选取的大小：${config.maxPicSize}M")
                    box.isChecked = false
                    return
                }
                val picCount = counts.first
                if (picCount >= config.maxPicSelect) {
                    ZToaster.makeTextS("图片最多可选取的数量：${config.maxPicSelect}")
                    box.isChecked = false
                } else {
                    selectedMap[item.path] = item
                    selectedArray.put(position, !selectedArray[position])
                    zPhotoSelectListener?.selected(selectedMap.size)

                }
            }
        }

    }

    /**
     * 获取已选中的图片和视频的数量
     */
    private fun getSelectPicOrVideoCount(): Pair<Int, Int> {
        var picCount = 0
        var videoCount = 0
        for ((_, v) in selectedMap) {
            if (v.isVideo) {
                videoCount ++
            } else {
                picCount ++
            }
        }
        return Pair(picCount, videoCount)
    }

    interface ZPhotoSelectListener {
        fun selected(selectedSize: Int)
    }
}