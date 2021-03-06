package com.vladimirkondenko.yamblz.dagger.modules;


import com.vladimirkondenko.yamblz.dagger.PerView;
import com.vladimirkondenko.yamblz.model.database.Database;
import com.vladimirkondenko.yamblz.model.database.DbSavedTranslationsServiceImpl;
import com.vladimirkondenko.yamblz.model.services.DbSavedTranslationsService;
import com.vladimirkondenko.yamblz.model.services.NetTranslationService;
import com.vladimirkondenko.yamblz.screens.translation.TranslationInteractor;
import com.vladimirkondenko.yamblz.screens.translation.TranslationPresenter;
import com.vladimirkondenko.yamblz.screens.translation.TranslationView;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;

@Module
public class TranslationModule extends BaseModule<TranslationView> {

    public TranslationModule(TranslationView view) {
        super(view);
    }

    @Provides
    @PerView
    public NetTranslationService provideTranslationService(Retrofit retrofit) {
        return retrofit.create(NetTranslationService.class);
    }

    @Provides
    @PerView
    public DbSavedTranslationsService provideDatabaseService(Database database) {
        return new DbSavedTranslationsServiceImpl(database);
    }

    @Provides
    @PerView
    public TranslationInteractor provideTranslationInteractor(NetTranslationService netService, DbSavedTranslationsService databaseService) {
        return new TranslationInteractor(netService, databaseService);
    }

    @Provides
    @PerView
    public TranslationPresenter provideTranslationPresenter(TranslationInteractor interactor) {
        return new TranslationPresenter(view, interactor);
    }

}
