/*
 * Copyright 2013 Inmite s.r.o. (www.inmite.eu).
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

package eu.inmite.android.lib.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Base dialog fragment for all your dialogs, stylable and same design on Android 2.2+.
 *
 * @author David Vávra (david@inmite.eu)
 */
public abstract class BaseDialogFragment extends DialogFragment {

    private Builder mBuilder;
    private OnDialogListener mOnDialogListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.SDL_Dialog);
        // custom dialog background
        final TypedArray a = getActivity().getTheme().obtainStyledAttributes(null, R.styleable.DialogStyle, R.attr.sdlDialogStyle, 0);
        Drawable dialogBackground = a.getDrawable(R.styleable.DialogStyle_dialogBackground);
        a.recycle();
        dialog.getWindow().setBackgroundDrawable(dialogBackground);
        Bundle args = getArguments();
        if (args != null) {
            dialog.setCanceledOnTouchOutside(args.getBoolean(BaseDialogBuilder.ARG_CANCELABLE_ON_TOUCH_OUTSIDE));
        }
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBuilder = new Builder(this, getActivity(), inflater, container);

        return build(mBuilder).create();
    }

    protected abstract Builder build(Builder initialBuilder);

    @Override
    public void onDestroyView() {
        // bug in the compatibility library
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        // mandatory to unregister mDataSetObserver for ListView
        mBuilder.onDialogDismiss();

        if (mOnDialogListener != null) {
            mOnDialogListener.onDialogDismiss();
        }

        mOnDialogListener = null;
        mBuilder = null;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);

        if (mOnDialogListener != null) {
            mOnDialogListener.onDialogShow();
        }
    }

    /**
     * Listener for {@link #show(android.support.v4.app.FragmentManager, String)}
     * and {@link #onDismiss(android.content.DialogInterface)} actions
     *
     * @param listener
     */
    public void setOnDialogListener(OnDialogListener listener) {
        mOnDialogListener = listener;
    }

    /**
     * @return the positive button if specified and the view is created, null otherwise
     */
    protected Button getPositiveButton() {
        if (getView() != null) {
            return (Button) getView().findViewById(R.id.sdl__positive_button);
        } else {
            return null;
        }
    }

    /**
     * @return the negative button if specified and the view is created, null otherwise
     */
    protected Button getNegativeButton() {
        if (getView() != null) {
            return (Button) getView().findViewById(R.id.sdl__negative_button);
        } else {
            return null;
        }
    }

    /**
     * @return the neutral button if specified and the view is created, null otherwise
     */
    protected Button getNeutralButton() {
        if (getView() != null) {
            return (Button) getView().findViewById(R.id.sdl__neutral_button);
        } else {
            return null;
        }
    }

    /**
     * Interface for show and onDismiss subscriber
     * See also {@link #show(android.support.v4.app.FragmentManager, String)}
     * and {@link #onDismiss(android.content.DialogInterface)}
     */
    public static interface OnDialogListener {

        public void onDialogShow();

        public void onDialogDismiss();
    }

    /**
     * Custom dialog builder
     */
    protected static class Builder implements OnDialogListener {

        private final DialogFragment mDialogFragment;
        private final Context mContext;
        private final ViewGroup mContainer;
        private final LayoutInflater mInflater;

        private CharSequence mTitle = null;
        private CharSequence mPositiveButtonText;
        private View.OnClickListener mPositiveButtonListener;
        private CharSequence mNegativeButtonText;
        private View.OnClickListener mNegativeButtonListener;
        private CharSequence mNeutralButtonText;
        private View.OnClickListener mNeutralButtonListener;
        private CharSequence mMessage;
        private View mView;
        private boolean mViewSpacingSpecified;
        private int mViewSpacingLeft;
        private int mViewSpacingTop;
        private int mViewSpacingRight;
        private int mViewSpacingBottom;
        private ListAdapter mListAdapter;
        private int mListCheckedItemIdx;
        private Object mActiveListItem;
        private AdapterView.OnItemClickListener mOnItemClickListener;
        private Drawable mIcon;
        private DataSetObserver mDataSetObserver;
        /**
         * Styling: *
         */
        private int mTitleTextColor;
        private int mTitleSeparatorColor;
        private int mMessageTextColor;
        private ColorStateList mButtonTextColor;
        private int mButtonSeparatorColor;
        private int mButtonBackgroundColorNormal;
        private int mButtonBackgroundColorPressed;
        private int mButtonBackgroundColorFocused;

        public Builder(DialogFragment dialogFragment, Context context, LayoutInflater inflater, ViewGroup container) {
            this.mDialogFragment = dialogFragment;
            this.mContext = context;
            this.mContainer = container;
            this.mInflater = inflater;
        }

        public LayoutInflater getLayoutInflater() {
            return mInflater;
        }

        public Builder setTitle(int titleId) {
            this.mTitle = mContext.getText(titleId);
            return this;
        }

        public Builder setTitle(CharSequence title) {
            this.mTitle = title;
            return this;
        }

        public Builder setPositiveButton(int textId, final View.OnClickListener listener) {
            mPositiveButtonText = mContext.getText(textId);
            mPositiveButtonListener = listener;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, final View.OnClickListener listener) {
            mPositiveButtonText = text;
            mPositiveButtonListener = listener;
            return this;
        }

        public Builder setNegativeButton(int textId, final View.OnClickListener listener) {
            mNegativeButtonText = mContext.getText(textId);
            mNegativeButtonListener = listener;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, final View.OnClickListener listener) {
            mNegativeButtonText = text;
            mNegativeButtonListener = listener;
            return this;
        }

        public Builder setNeutralButton(int textId, final View.OnClickListener listener) {
            mNeutralButtonText = mContext.getText(textId);
            mNeutralButtonListener = listener;
            return this;
        }

        public Builder setNeutralButton(CharSequence text, final View.OnClickListener listener) {
            mNeutralButtonText = text;
            mNeutralButtonListener = listener;
            return this;
        }

        public Builder setMessage(int messageId) {
            mMessage = mContext.getText(messageId);
            return this;
        }

        public Builder setMessage(CharSequence message) {
            mMessage = message;
            return this;
        }

        /**
         * Set list
         *
         * @param listAdapter
         * @param checkedItemIdx Item check by default, -1 if no item should be checked
         * @param listener
         * @return
         */
        public Builder setItems(ListAdapter listAdapter, int checkedItemIdx, final AdapterView.OnItemClickListener listener) {
            mListAdapter = listAdapter;
            mOnItemClickListener = listener;
            mListCheckedItemIdx = checkedItemIdx;
            return this;
        }

        /**
         * Set list
         *
         * @param listAdapter
         * @param activeItem  Item check by default, even if the ListView is not populated, yet.
         * @param listener
         * @return
         */
        public Builder setItems(ListAdapter listAdapter, Object activeItem, final AdapterView.OnItemClickListener listener) {
            mListAdapter = listAdapter;
            mOnItemClickListener = listener;
            mActiveListItem = activeItem;
            return this;
        }

        public Builder setView(View view) {
            mView = view;
            mViewSpacingSpecified = false;
            return this;
        }

        public Builder setView(View view, int viewSpacingLeft, int viewSpacingTop,
                               int viewSpacingRight, int viewSpacingBottom) {
            mView = view;
            mViewSpacingSpecified = true;
            mViewSpacingLeft = viewSpacingLeft;
            mViewSpacingTop = viewSpacingTop;
            mViewSpacingRight = viewSpacingRight;
            mViewSpacingBottom = viewSpacingBottom;
            return this;
        }

        public Builder setIcon(int resourceId) {
            mIcon = mContext.getResources().getDrawable(resourceId);
            return this;
        }

        public Builder setIcon(Drawable drawable) {
            mIcon = drawable;
            return this;
        }

        public View create() {
            final Resources res = mContext.getResources();
            final int defaultTitleTextColor = res.getColor(R.color.sdl_title_text_dark);
            final int defaultTitleSeparatorColor = res.getColor(R.color.sdl_title_separator_dark);
            final int defaultMessageTextColor = res.getColor(R.color.sdl_message_text_dark);
            final ColorStateList defaultButtonTextColor = res.getColorStateList(R.color.sdl_button_text_dark);
            final int defaultButtonSeparatorColor = res.getColor(R.color.sdl_button_separator_dark);
            final int defaultButtonBackgroundColorNormal = res.getColor(R.color.sdl_button_normal_dark);
            final int defaultButtonBackgroundColorPressed = res.getColor(R.color.sdl_button_pressed_dark);
            final int defaultButtonBackgroundColorFocused = res.getColor(R.color.sdl_button_focused_dark);

            final TypedArray a = mContext.getTheme().obtainStyledAttributes(null, R.styleable.DialogStyle, R.attr.sdlDialogStyle, 0);
            mTitleTextColor = a.getColor(R.styleable.DialogStyle_titleTextColor, defaultTitleTextColor);
            mTitleSeparatorColor = a.getColor(R.styleable.DialogStyle_titleSeparatorColor, defaultTitleSeparatorColor);
            mMessageTextColor = a.getColor(R.styleable.DialogStyle_messageTextColor, defaultMessageTextColor);
            mButtonTextColor = a.getColorStateList(R.styleable.DialogStyle_buttonTextColor);
            if (mButtonTextColor == null) {
                mButtonTextColor = defaultButtonTextColor;
            }
            mButtonSeparatorColor = a.getColor(R.styleable.DialogStyle_buttonSeparatorColor, defaultButtonSeparatorColor);
            mButtonBackgroundColorNormal = a.getColor(R.styleable.DialogStyle_buttonBackgroundColorNormal, defaultButtonBackgroundColorNormal);
            mButtonBackgroundColorPressed = a.getColor(R.styleable.DialogStyle_buttonBackgroundColorPressed, defaultButtonBackgroundColorPressed);
            mButtonBackgroundColorFocused = a.getColor(R.styleable.DialogStyle_buttonBackgroundColorFocused, defaultButtonBackgroundColorFocused);
            a.recycle();

            View v = getDialogLayoutAndInitTitle();

            LinearLayout content = (LinearLayout) v.findViewById(R.id.sdl__content);

            if (mMessage != null) {
                View viewMessage = mInflater.inflate(R.layout.dialog_part_message, content, false);
                TextView tvMessage = (TextView) viewMessage.findViewById(R.id.sdl__message);
                tvMessage.setTextColor(mMessageTextColor);
                tvMessage.setText(mMessage);
                content.addView(viewMessage);
            }

            if (mView != null) {
                FrameLayout customPanel = (FrameLayout) mInflater.inflate(R.layout.dialog_part_custom, content, false);
                FrameLayout custom = (FrameLayout) customPanel.findViewById(R.id.sdl__custom);
                custom.addView(mView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                if (mViewSpacingSpecified) {
                    custom.setPadding(mViewSpacingLeft, mViewSpacingTop, mViewSpacingRight, mViewSpacingBottom);
                }
                content.addView(customPanel);
            }

            if (mListAdapter != null) {
                ListView list = (ListView) mInflater.inflate(R.layout.dialog_part_list, content, false);
                list.setAdapter(mListAdapter);
                list.setOnItemClickListener(mOnItemClickListener);

                // register a data observer, to handle an active item for changing ListAdapter entries
                if (mActiveListItem != null) {
                    mDataSetObserver = new ListDataSetObserver(list, mActiveListItem, mListAdapter);
                    mListAdapter.registerDataSetObserver(mDataSetObserver);
                } else if (mListCheckedItemIdx != -1) {
                    list.setItemChecked(mListCheckedItemIdx, true);
                }

                content.addView(list);
            }

            addButtons(content);

            return v;
        }

        private View getDialogLayoutAndInitTitle() {
            View v = mInflater.inflate(R.layout.dialog_part_title, mContainer, false);
            TextView tvTitle = (TextView) v.findViewById(R.id.sdl__title);
            View viewTitleDivider = v.findViewById(R.id.sdl__titleDivider);
            if (mTitle != null) {
                tvTitle.setText(mTitle);
                tvTitle.setTextColor(mTitleTextColor);
                if (mIcon != null) {
                    tvTitle.setCompoundDrawablesWithIntrinsicBounds(mIcon, null, null, null);
                    tvTitle.setCompoundDrawablePadding(mContext.getResources().getDimensionPixelSize(R.dimen.grid_2));
                }
                viewTitleDivider.setBackgroundDrawable(new ColorDrawable(mTitleSeparatorColor));
            } else {
                tvTitle.setVisibility(View.GONE);
                viewTitleDivider.setVisibility(View.GONE);
            }
            return v;
        }

        private void addButtons(LinearLayout llListDialog) {
            if (mNegativeButtonText != null || mNeutralButtonText != null || mPositiveButtonText != null) {
                View viewButtonPanel = mInflater.inflate(R.layout.dialog_part_button_panel, llListDialog, false);
                LinearLayout llButtonPanel = (LinearLayout) viewButtonPanel.findViewById(R.id.dialog_button_panel);
                viewButtonPanel.findViewById(R.id.dialog_horizontal_separator).setBackgroundDrawable(new ColorDrawable(mButtonSeparatorColor));

                boolean addDivider = false;

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    addDivider = addPositiveButton(llButtonPanel, addDivider);
                } else {
                    addDivider = addNegativeButton(llButtonPanel, addDivider);
                }
                addDivider = addNeutralButton(llButtonPanel, addDivider);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    addNegativeButton(llButtonPanel, addDivider);
                } else {
                    addPositiveButton(llButtonPanel, addDivider);
                }

                llListDialog.addView(viewButtonPanel);
            }
        }

        private boolean addNegativeButton(ViewGroup parent, boolean addDivider) {
            if (mNegativeButtonText != null) {
                if (addDivider) {
                    addDivider(parent);
                }
                Button btn = (Button) mInflater.inflate(R.layout.dialog_part_button, parent, false);
                btn.setId(R.id.sdl__negative_button);
                btn.setText(mNegativeButtonText);
                btn.setTextColor(mButtonTextColor);
                btn.setBackgroundDrawable(getButtonBackground());
                btn.setOnClickListener(mNegativeButtonListener);
                parent.addView(btn);
                return true;
            }
            return addDivider;
        }

        private boolean addPositiveButton(ViewGroup parent, boolean addDivider) {
            if (mPositiveButtonText != null) {
                if (addDivider) {
                    addDivider(parent);
                }
                Button btn = (Button) mInflater.inflate(R.layout.dialog_part_button, parent, false);
                btn.setId(R.id.sdl__positive_button);
                btn.setText(mPositiveButtonText);
                btn.setTextColor(mButtonTextColor);
                btn.setBackgroundDrawable(getButtonBackground());
                btn.setOnClickListener(mPositiveButtonListener);
                parent.addView(btn);
                return true;
            }
            return addDivider;
        }

        private boolean addNeutralButton(ViewGroup parent, boolean addDivider) {
            if (mNeutralButtonText != null) {
                if (addDivider) {
                    addDivider(parent);
                }
                Button btn = (Button) mInflater.inflate(R.layout.dialog_part_button, parent, false);
                btn.setId(R.id.sdl__neutral_button);
                btn.setText(mNeutralButtonText);
                btn.setTextColor(mButtonTextColor);
                btn.setBackgroundDrawable(getButtonBackground());
                btn.setOnClickListener(mNeutralButtonListener);
                parent.addView(btn);
                return true;
            }
            return addDivider;
        }

        private void addDivider(ViewGroup parent) {
            View view = mInflater.inflate(R.layout.dialog_part_button_separator, parent, false);
            view.findViewById(R.id.dialog_button_separator).setBackgroundDrawable(new ColorDrawable(mButtonSeparatorColor));
            parent.addView(view);
        }

        private StateListDrawable getButtonBackground() {
            int[] pressedState = {android.R.attr.state_pressed};
            int[] focusedState = {android.R.attr.state_focused};
            int[] defaultState = {android.R.attr.state_enabled};
            ColorDrawable colorDefault = new ColorDrawable(mButtonBackgroundColorNormal);
            ColorDrawable colorPressed = new ColorDrawable(mButtonBackgroundColorPressed);
            ColorDrawable colorFocused = new ColorDrawable(mButtonBackgroundColorFocused);
            StateListDrawable background = new StateListDrawable();
            background.addState(pressedState, colorPressed);
            background.addState(focusedState, colorFocused);
            background.addState(defaultState, colorDefault);
            return background;
        }

        @Override
        public void onDialogShow() {
        }

        @Override
        public void onDialogDismiss() {
            if (mListAdapter != null && mDataSetObserver != null) {
                mListAdapter.unregisterDataSetObserver(mDataSetObserver);
            }
        }

        private static class ListDataSetObserver extends DataSetObserver {

            private ListView mInternalListView;
            private Object mInternalActiveListItem;
            private ListAdapter mInternalListAdapter;

            public ListDataSetObserver(ListView listView, Object activeItem, ListAdapter adapter) {
                mInternalListView = listView;
                mInternalActiveListItem = activeItem;
                mInternalListAdapter = adapter;

                onChanged();
            }

            @Override
            public void onChanged() {
                mInternalListView.setItemChecked(getItemPosition(mInternalActiveListItem, mInternalListAdapter), true);
            }

            private int getItemPosition(Object item, ListAdapter adapter) {
                int position;

                for (position = 0; position < adapter.getCount(); position++) {
                    if (adapter.getItem(position).equals(item)) {
                        return position;
                    }
                }
                return -1;
            }
        }
    }
}
