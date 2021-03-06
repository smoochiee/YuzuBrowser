/*
 * Copyright (C) 2017 Hazuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.hazuki.yuzubrowser.action.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Toast
import jp.hazuki.yuzubrowser.BrowserApplication
import jp.hazuki.yuzubrowser.R
import jp.hazuki.yuzubrowser.action.ActionManager
import jp.hazuki.yuzubrowser.action.ActionNameArray
import jp.hazuki.yuzubrowser.action.SoftButtonActionArrayManagerBase
import jp.hazuki.yuzubrowser.action.manager.SoftButtonActionArrayFile
import jp.hazuki.yuzubrowser.action.manager.SoftButtonActionFile
import jp.hazuki.yuzubrowser.utils.view.DeleteDialogCompat
import jp.hazuki.yuzubrowser.utils.view.recycler.ArrayRecyclerAdapter
import jp.hazuki.yuzubrowser.utils.view.recycler.OnRecyclerListener
import jp.hazuki.yuzubrowser.utils.view.recycler.RecyclerFabFragment
import jp.hazuki.yuzubrowser.utils.view.recycler.SimpleViewHolder

class SoftButtonActionArrayFragment : RecyclerFabFragment(), OnRecyclerListener, DeleteDialogCompat.OnDelete {

    private var mActionType: Int = 0
    private var mActionId: Int = 0
    private lateinit var mActionArray: SoftButtonActionArrayFile
    private lateinit var actionManager: SoftButtonActionArrayManagerBase
    private lateinit var adapter: ActionListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: return

        setHasOptionsMenu(true)
        initData()
        val mActionNameArray = ActionNameArray(activity)
        adapter = ActionListAdapter(activity, mActionArray.list, mActionNameArray, this)
        setRecyclerViewAdapter(adapter)
        checkMax()
    }

    override fun onMove(recyclerView: RecyclerView, fromIndex: Int, toIndex: Int): Boolean {
        adapter.move(fromIndex, toIndex)
        return true
    }

    override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
        mActionArray.write(BrowserApplication.getInstance())
    }

    override fun onRecyclerItemClicked(v: View, position: Int) {
        onListItemClick(position)
    }

    override fun onRecyclerItemLongClicked(v: View, position: Int): Boolean {
        DeleteDialogCompat.newInstance(activity, R.string.confirm, R.string.confirm_delete_button, position)
                .show(childFragmentManager, "delete")
        return true
    }

    override fun onDelete(position: Int) {
        mActionArray.list.removeAt(position)
        mActionArray.write(BrowserApplication.getInstance())
        adapter.notifyDataSetChanged()
    }

    override fun onAddButtonClick() {
        mActionArray.list.add(SoftButtonActionFile())
        mActionArray.write(BrowserApplication.getInstance())
        onListItemClick(mActionArray.list.size - 1)
    }

    private fun onListItemClick(position: Int) {
        val activity = activity ?: return

        val intent = SoftButtonActionActivity.Builder(activity)
                .setActionManager(mActionType, actionManager.makeActionIdFromPosition(mActionId, position))
                .setTitle(activity.title.toString() + " - " + (position + 1).toString())
                .create()
        startActivityForResult(intent, RESULT_REQUEST_ADD)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, index: Int) {
        val file = mActionArray.list.removeAt(index)
        adapter.notifyDataSetChanged()
        checkMax()
        Snackbar.make(rootView, R.string.deleted, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo) {
                    mActionArray.list.add(index, file)
                    adapter.notifyDataSetChanged()
                    checkMax()
                }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            mActionArray.write(BrowserApplication.getInstance())
                        }
                    }
                })
                .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RESULT_REQUEST_ADD -> {
                adapter.notifyDataSetChanged()
                checkMax()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.sort, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> {
                val next = !adapter.isSortMode
                adapter.isSortMode = next

                Toast.makeText(activity, if (next) R.string.start_sort else R.string.end_sort, Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return false
    }

    override val isLongPressDragEnabled
        get() = adapter.isSortMode

    private fun checkMax() {
        setAddButtonEnabled(actionManager.max >= adapter.itemCount)
    }

    private fun initData() {
        val arguments = arguments ?: throw IllegalArgumentException()

        mActionType = arguments.getInt(ACTION_TYPE)
        mActionId = arguments.getInt(ACTION_ID)

        actionManager = ActionManager.getActionManager(BrowserApplication.getInstance(), mActionType) as? SoftButtonActionArrayManagerBase ?: throw IllegalArgumentException()

        mActionArray = actionManager.getActionArrayFile(mActionId)
    }

    private class ActionListAdapter internal constructor(context: Context, list: MutableList<SoftButtonActionFile>, private val actionNameArray: ActionNameArray, listener: OnRecyclerListener) : ArrayRecyclerAdapter<SoftButtonActionFile, SimpleViewHolder<SoftButtonActionFile>>(context, list, listener) {

        override fun onBindViewHolder(holder: SimpleViewHolder<SoftButtonActionFile>, item: SoftButtonActionFile, position: Int) {
            holder.textView.text = item.press.toString(actionNameArray)
        }

        override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup?, viewType: Int): SimpleViewHolder<SoftButtonActionFile> {
            return SimpleViewHolder(
                    inflater.inflate(R.layout.simple_recycler_list_item_1, parent, false), android.R.id.text1, this)
        }


    }

    companion object {
        private const val ACTION_TYPE = "type"
        private const val ACTION_ID = "id"
        private const val RESULT_REQUEST_ADD = 1

        fun newInstance(actionType: Int, actionId: Int): Fragment {
            return SoftButtonActionArrayFragment().apply {
                arguments = Bundle().apply {
                    putInt(ACTION_TYPE, actionType)
                    putInt(ACTION_ID, actionId)
                }
            }
        }
    }
}
