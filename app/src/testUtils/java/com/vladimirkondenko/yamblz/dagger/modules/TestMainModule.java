package com.vladimirkondenko.yamblz.dagger.modules;

import android.content.Context;

import com.vladimirkondenko.yamblz.dagger.PerView;
import com.vladimirkondenko.yamblz.model.services.NetLanguagesService;
import com.vladimirkondenko.yamblz.screens.main.MainInteractor;
import com.vladimirkondenko.yamblz.screens.main.MainPresenter;
import com.vladimirkondenko.yamblz.screens.main.MainView;

import org.mockito.Mockito;

import dagger.Module;
import dagger.Provides;

@Module
public class TestMainModule extends BaseModule<MainView> {

    public TestMainModule(MainView view) {
        super(view);
    }

    @Provides
    @PerView
    public NetLanguagesService provideAvailableLanguagesService() {
        return Mockito.mock(NetLanguagesService.class);
    }

    @Provides
    @PerView
    public MainInteractor provideMainInteractor(Context context, NetLanguagesService service) {
        return Mockito.mock(MainInteractor.class);
    }

    @Provides
    @PerView
    public MainPresenter provideMainPresenter(MainInteractor interactor) {
        return new MainPresenter(view, interactor);
    }

}
