package com.vladimirkondenko.yamblz.screens.history;


import android.animation.ObjectAnimator;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;

import com.jakewharton.rxbinding2.widget.RxRadioGroup;
import com.vladimirkondenko.yamblz.App;
import com.vladimirkondenko.yamblz.Const;
import com.vladimirkondenko.yamblz.R;
import com.vladimirkondenko.yamblz.dagger.modules.HistoryModule;
import com.vladimirkondenko.yamblz.databinding.FragmentHistoryBinding;
import com.vladimirkondenko.yamblz.model.entities.Translation;
import com.vladimirkondenko.yamblz.utils.Utils;
import com.vladimirkondenko.yamblz.utils.adapters.TranslationsAdapter;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import io.realm.OrderedRealmCollection;

import static android.R.attr.y;

public class HistoryFragment extends Fragment implements HistoryView {

    static {
        // VectorDrawable support for pre-Lollipop devices
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final String TAG = "HistoryFragment";

    @Inject
    public HistoryPresenter presenter;

    private FragmentHistoryBinding binding;

    private Disposable tabsSubscription;

    public HistoryFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_history, container, false);
        App.get().plusHistorySubcomponent(new HistoryModule(this)).inject(this);
        // RadioButton icons
        Drawable historyDrawable = Utils.getTintedIcon(getContext(), R.drawable.ic_history_black_24px);
        Drawable bookmarkDrawable = Utils.getTintedIcon(getContext(), R.drawable.ic_bookmark_black_24px);
        binding.radiobuttonHistoryTabHistory.setCompoundDrawablesRelativeWithIntrinsicBounds(historyDrawable, null, null, null);
        binding.radiobuttonHistoryTabBookmarks.setCompoundDrawablesRelativeWithIntrinsicBounds(bookmarkDrawable, null, null, null);
        // Tabs
        tabsSubscription = RxRadioGroup.checkedChanges(binding.radiogroupHistoryTabs)
                .subscribe(integer -> presenter.selectTab(TabCodes.tabToScreenId(integer)));
        // RecyclerView
        binding.recyclerviewTranslations.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerviewTranslations.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        presenter.attachView(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        animateBottomTabs(true);
    }

    @Override
    public void onDestroyView() {
        animateBottomTabs(false);
        super.onDestroyView();
        presenter.detachView();
        Utils.dispose(tabsSubscription);
        App.get().clearHistorySubcomponent();
    }

    @Override
    public void onHistorySelected() {
        this.getActivity().setTitle(R.string.history_title_history);
    }

    @Override
    public void onBookmarksSelected() {
        this.getActivity().setTitle(R.string.history_title_bookmarks);
    }

    @Override
    public void displayList(OrderedRealmCollection<Translation> translations) {
        TranslationsAdapter adapter = new TranslationsAdapter(presenter.getAdapterPresenter(), translations);
        binding.recyclerviewTranslations.setAdapter(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                presenter.remove(adapter.getData().get(position));
            }
        });
        itemTouchHelper.attachToRecyclerView(binding.recyclerviewTranslations);
    }

    private void animateBottomTabs(boolean in) {
        View view = binding.radiogroupHistoryTabs;
        /*
        float height = view.getHeight() + view.getPaddingBottom();
        Interpolator interpolator = in ? new DecelerateInterpolator() : new AccelerateInterpolator();
        Animation anim = new TranslateAnimation(0, 0, 0f, 1f);
        anim.setDuration(Const.ANIM_DURATION_HISTORY_TABS_HIDE);
        anim.setInterpolator(interpolator);
        view.startAnimation(anim);
        */
//        float offScreen = (in ? -1 : 1) * binding.radiogroupHistoryTabs.getHeight();
//        float fromValue = in ? 0f : offScreen;
//        float toValue = in ? offScreen : 0f;
//        binding.radiogroupHistoryTabs.animate().y().translationY(in ? 1f : -1f).
        float height = view.getHeight() + view.getPaddingBottom();
        float y = in ? binding.getRoot().getBottom() : view.getTop();
        Interpolator interpolator = in ? new DecelerateInterpolator() : new AccelerateInterpolator();
        view.animate()
                .setDuration(Const.ANIM_DURATION_HISTORY_TABS_HIDE)
                .setInterpolator(interpolator)
                .y(y)
                .translationY(in ? -height : height)
                .start();
    }

}
