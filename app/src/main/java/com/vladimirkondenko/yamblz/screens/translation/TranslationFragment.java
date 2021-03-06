package com.vladimirkondenko.yamblz.screens.translation;


import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.vladimirkondenko.yamblz.App;
import com.vladimirkondenko.yamblz.Const;
import com.vladimirkondenko.yamblz.R;
import com.vladimirkondenko.yamblz.dagger.modules.TranslationModule;
import com.vladimirkondenko.yamblz.databinding.FragmentTranslationBinding;
import com.vladimirkondenko.yamblz.model.entities.Translation;
import com.vladimirkondenko.yamblz.utils.ErrorCodes;
import com.vladimirkondenko.yamblz.utils.LanguageUtils;
import com.vladimirkondenko.yamblz.utils.RxNetworkBroadcastReceiver;
import com.vladimirkondenko.yamblz.utils.Utils;
import com.vladimirkondenko.yamblz.utils.events.Bus;
import com.vladimirkondenko.yamblz.utils.events.InputLanguageSelectionEvent;
import com.vladimirkondenko.yamblz.utils.events.LanguageDetectionEvent;
import com.vladimirkondenko.yamblz.utils.events.OutputLanguageSelectionEvent;
import com.vladimirkondenko.yamblz.utils.events.SelectLanguageEvent;
import com.vladimirkondenko.yamblz.utils.events.SwapLanguageEvent;
import com.vladimirkondenko.yamblz.utils.ui.AnimUtils;
import com.vladimirkondenko.yamblz.utils.ui.RxCheckableImageButton;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * The main fragment which provides primary translator features.
 */
public class TranslationFragment extends Fragment implements TranslationView {

    @Inject
    public RxNetworkBroadcastReceiver networkBroadcastReceiver;

    @Inject
    public TranslationPresenter presenter;

    private FragmentTranslationBinding binding;

    private Disposable subscriptionClearButton;
    private Disposable subscriptionSelectDetectedLang;
    private Disposable subscriptionInputTextChanges;
    private Disposable subscriptionInputTextEvents;
    private Disposable subscriptionBookmarkClicks;

    public TranslationFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_translation, container, false);
        App.get().plusTranslationSubcomponent(new TranslationModule(this)).inject(this);
        presenter.attachView(this);
        Bus.subscribe(this);
        networkBroadcastReceiver.register().subscribe(isOnline -> {
            binding.includeTranslationOfflineBanner.linearlayoutTranslationOfflineBannerRoot.setVisibility(isOnline ? View.GONE : View.VISIBLE);
            if (isOnline) presenter.executePendingTranslation();
        });
        Drawable bookmarkDrawable = Utils.getTintedIcon(getContext(), R.drawable.selector_all_bookmark);
        binding.includeTranslationBookmarkButton.buttonTransationBookmark.setImageDrawable(bookmarkDrawable);
        subscriptionBookmarkClicks = RxCheckableImageButton.checks(binding.includeTranslationBookmarkButton.buttonTransationBookmark)
                .subscribe(presenter::bookmarkTranslation);
        subscriptionClearButton = RxView.clicks(binding.buttonTranslationClearInput)
                .subscribe(o -> presenter.clickClearButton());
        subscriptionInputTextChanges = RxTextView.textChanges(binding.edittextTranslationInput)
                .skipInitialValue()
                .debounce(225, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .filter(text -> text.length() < Const.MAX_TEXT_LENGTH)
                .map(String::valueOf)
                .map(String::trim)
                .subscribe(text -> presenter.onInputTextChange(text, networkBroadcastReceiver.isOnline()));
        subscriptionInputTextEvents = RxTextView.editorActions(binding.edittextTranslationInput)
                .subscribe(event -> presenter.pressEnter());
        presenter.onCreateView();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.detachView();
        Utils.disposeAll(
                subscriptionClearButton,
                subscriptionInputTextChanges,
                subscriptionSelectDetectedLang,
                subscriptionInputTextEvents,
                subscriptionBookmarkClicks
        );
        networkBroadcastReceiver.unregister();
        Bus.unsubscribe(this);
        App.get().clearTranslationPresenterComponent();
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSwapLanguages(SwapLanguageEvent event) {
        if (!Utils.areFieldsEmpty(binding.edittextTranslationInput, binding.textviewTranslationResult)) {
            int duration = Const.ANIM_DURATION_LANG_SWITCH_SPINNER;
            int distance = 4;
            AnimUtils.slideInAndOut(
                    binding.edittextTranslationInput,
                    true,
                    distance,
                    duration,
                    () -> {
                    },
                    () -> binding.edittextTranslationInput.setText(binding.textviewTranslationResult.getText())
            );
            AnimUtils.slideInAndOut(
                    binding.textviewTranslationResult,
                    false,
                    distance,
                    duration,
                    () -> {
                    },
                    () -> binding.textviewTranslationResult.setText("")
            );
        }
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDetectLanguage(LanguageDetectionEvent event) {
        String langCode = LanguageUtils.parseDirection(event.getDetectedLang())[0];
        Locale locale = new Locale(langCode);
        String language = locale.getDisplayLanguage();
        binding.textviewDetectedLang.setText(language);
        showDetectedLangLayout(true);
        subscriptionSelectDetectedLang = RxView.clicks(binding.framelayoutDetectedLang)
                .subscribe(o -> {
                    showDetectedLangLayout(false);
                    Bus.post(new SelectLanguageEvent(langCode));
                });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInputLangChange(InputLanguageSelectionEvent event) {
        presenter.selectInputLanguage(event.getInputLang());
        showDetectedLangLayout(false);
        presenter.saveLastTranslation();
        translate();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOutputLangChange(OutputLanguageSelectionEvent event) {
        presenter.selectOutputLanguage(event.getOutputLang());
        presenter.saveLastTranslation();
        translate();
    }

    @Override
    public void onTranslationSuccess(Translation translation) {
        binding.textviewTranslationResult.setText(translation.getFormattedTranslatedText());
        binding.includeTranslationBookmarkButton.buttonTransationBookmark.setChecked(translation.isBookmarked());
    }

    @Override
    public void onError(Throwable t, int errorCode) {
        if (t != null) {
            t.printStackTrace();
        }
        displayErrorMessage(errorCode);
    }

    @Override
    public void onClearButtonClicked() {
        binding.edittextTranslationInput.getText().clear();
        binding.textviewTranslationResult.setText("");
        showDetectedLangLayout(false);
    }

    @Override
    public void onTextCleared() {
        showDetectedLangLayout(false);
        binding.textviewTranslationResult.setText("");
    }

    @Override
    public void onEnterKeyPressed() {
        hideKeyboard();
    }

    @Override
    public void onBookmarkingEnabled(boolean enabled) {
        binding.includeTranslationBookmarkButton.buttonTransationBookmark.setEnabled(enabled);
        if (!enabled)
            binding.includeTranslationBookmarkButton.buttonTransationBookmark.setChecked(false);
    }

    private void displayErrorMessage(int errorCode) {
        int errorMessageResId;
        switch (errorCode) {
            case ErrorCodes.TEXT_TOO_LONG: {
                errorMessageResId = R.string.translation_error_text_too_long;
            }
            default: {
                errorMessageResId = R.string.all_error_generic;
            }
        }
        Toast.makeText(getContext(), errorMessageResId, Toast.LENGTH_SHORT).show();
    }

    private void translate() {
        if (!Utils.isEmpty(binding.edittextTranslationInput) && presenter != null) {
            String text = String.valueOf(binding.edittextTranslationInput.getText());
            presenter.enqueueTranslation(text);
            if (networkBroadcastReceiver.isOnline()) presenter.executePendingTranslation();
        }
    }

    private void showDetectedLangLayout(boolean show) {
        binding.framelayoutDetectedLang.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.edittextTranslationInput.getWindowToken(), 0);
    }

}
