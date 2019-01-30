package xyz.vola.openinbrowser

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_browsers_list_dialog.*
import kotlinx.android.synthetic.main.fragment_browsers_list_dialog_item.view.*

const val ARGS_ICT_PACKAGES = "browsers_ict"
const val ARGS_STANDALONE_PACKAGES = "browsers_better_in_standalone"

/**
 * 打开一个 Bottom sheet 选择器
 *
 * <pre>
 *    BrowsersListDialogFragment.newInstance(ICTs: ResolveInfo[], standalone: ResolveInfo[]).show(supportFragmentManager, "dialog")
 * </pre>
 *
 * 打开者需要实现 [BrowsersListDialogFragment.Listener]
 */
class BrowsersListDialogFragment : BottomSheetDialogFragment() {
    private lateinit var mListener: Listener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browsers_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val packageManager = context!!.packageManager
        list_ict.layoutManager = LinearLayoutManager(context)
        list_trusted.layoutManager = LinearLayoutManager(context)

        val ct = arguments?.getStringArray(ARGS_ICT_PACKAGES) ?: arrayOf()
        list_ict.adapter = BrowsersAdapter(packageManager, ct)

        val standalone = arguments?.getStringArray(ARGS_STANDALONE_PACKAGES) ?: arrayOf()
        list_trusted.adapter = BrowsersAdapter(packageManager, standalone)
        /** 隐藏空的列表 */
        val ictVisibility = if (ct.isEmpty()) View.GONE else View.VISIBLE
        list_ict.visibility = ictVisibility
        open_in_custom_tab.visibility = ictVisibility

        val standaloneVisibility = if (standalone.isEmpty()) View.GONE else View.VISIBLE
        list_trusted.visibility = standaloneVisibility
        open_in_trusted.visibility = standaloneVisibility

        if (standalone.isEmpty() && ct.isEmpty()) {
            Toast.makeText(context, R.string.dialog_no_browser_found, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        mListener = if (parent != null) parent as Listener else context as Listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mListener.onDialogDismissed()
    }

    interface Listener {
        /** 浏览器对 ICT 的支持情况 */
        enum class SupportingTypeOfICT {
            /** 支持 */
            Supported,
            /** 未知 */
            Unknown,
            /** 不支持 */
            NotSupport,
            /** 忽略 CT 支持，直接启动应用 */
            Ignore
        }

        fun onBrowsersClicked(packageName: String)
        fun isCustomTabBrowserSupportIncognito(packageManager: PackageManager, packageName: String): SupportingTypeOfICT
        fun onDialogDismissed()
    }

    private inner class ViewHolder internal constructor(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_browsers_list_dialog_item, parent, false)) {
        internal val applicationName = itemView.browser_name_with_caption
        internal val applicationName2 = itemView.browser_name_without_caption
        internal val applicationIcon = itemView.browser_icon
        internal val captionMaybe = itemView.caption_maybe
        internal val captionNo = itemView.caption_no
        internal val row = itemView.dialog_row
        internal val packageSlot = itemView.package_name

        internal val withCaption = itemView.layout_with_caption
        internal val withOutCaption = itemView.layout_without_caption

        init {
            row.setOnClickListener {
                mListener.onBrowsersClicked(packageSlot.text.toString())
                dismiss()
            }
            row.setOnLongClickListener {
                val packageName = packageSlot.text.toString()
                Toast.makeText(
                    activity,
                    packageName + " " + context?.packageManager?.getPackageInfo(packageName, 0)?.longVersionCode,
                    Toast.LENGTH_LONG
                ).show()
                true
            }
        }
    }

    private enum class Type { WithCaption, WithoutCaption }
    private inner class BrowsersAdapter internal constructor(
        private val mPackageManager: PackageManager,
        private val mPackageName: Array<String>
    ) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context), parent)

        fun getLayoutType(position: Int): Type {
            return when (mListener.isCustomTabBrowserSupportIncognito(mPackageManager, mPackageName[position])) {
                Listener.SupportingTypeOfICT.Unknown -> Type.WithCaption
                Listener.SupportingTypeOfICT.NotSupport -> Type.WithCaption
                Listener.SupportingTypeOfICT.Supported -> Type.WithoutCaption
                Listener.SupportingTypeOfICT.Ignore -> Type.WithoutCaption
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val applicationInfo = mPackageManager.getApplicationInfo(mPackageName[position], 0)
            val applicationLabel = mPackageManager.getApplicationLabel(applicationInfo)
            val applicationIcon = mPackageManager.getApplicationIcon(applicationInfo)

            // 不可见的 label，给回调使用的信息
            holder.packageSlot.text = applicationInfo.packageName
            // 可见的内容
            holder.applicationIcon.setImageDrawable(applicationIcon)
            holder.applicationName.text = applicationLabel
            holder.applicationName2.text = applicationLabel

            when (getLayoutType(position)) {
                Type.WithCaption -> {
                    holder.withOutCaption.visibility = View.GONE
                }
                Type.WithoutCaption -> {
                    holder.withCaption.visibility = View.GONE
                    // 把另一种警告隐藏
                    (if (mListener.isCustomTabBrowserSupportIncognito(
                            mPackageManager,
                            mPackageName[position]
                        ) == Listener.SupportingTypeOfICT.Unknown
                    )
                        holder.captionNo
                    else holder.captionMaybe).visibility = View.GONE
                }
            }
        }

        override fun getItemCount(): Int {
            return mPackageName.size
        }
    }

    companion object {
        fun newInstance(
            CTBrowsers: List<ResolveInfo>,
            StandaloneBrowsers: List<ResolveInfo>
        ): BrowsersListDialogFragment =
            BrowsersListDialogFragment().apply {
                arguments = Bundle().apply {
                    putStringArray(
                        ARGS_ICT_PACKAGES,
                        Array(CTBrowsers.size) { x -> CTBrowsers[x].activityInfo.packageName })
                    putStringArray(
                        ARGS_STANDALONE_PACKAGES,
                        Array(StandaloneBrowsers.size) { x -> StandaloneBrowsers[x].activityInfo.packageName })
                }
            }
    }
}
