package com.cubiquitous.tracura.di

import android.content.Context
import androidx.work.WorkManager
import com.cubiquitous.tracura.repository.ExpenseRepository
import com.cubiquitous.tracura.repository.ExportRepository
import com.cubiquitous.tracura.repository.ProfessionalExportRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.cubiquitous.tracura.repository.NotificationRepository
import com.cubiquitous.tracura.repository.AuthRepository
import com.cubiquitous.tracura.repository.TemporaryApproverRepository
import com.cubiquitous.tracura.service.NotificationService
import com.cubiquitous.tracura.service.DelegationExpiryService
import com.cubiquitous.tracura.service.DelegationScheduler
import com.cubiquitous.tracura.service.ProfessionalReportGenerator
import com.cubiquitous.tracura.service.FCMPushNotificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.Lazy
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance()
    
    @Provides
    @Singleton
    fun provideExpenseRepository(
        firestore: FirebaseFirestore,
        projectRepository: ProjectRepository
    ): ExpenseRepository {
        return ExpenseRepository(firestore, projectRepository)
    }
    
    @Provides
    @Singleton
    fun provideTemporaryApproverRepository(
        firestore: FirebaseFirestore,
        notificationService: Lazy<NotificationService>
    ): TemporaryApproverRepository {
        return TemporaryApproverRepository(firestore, notificationService)
    }
    
    @Provides
    @Singleton
    fun provideProjectRepository(
        firestore: FirebaseFirestore,
        temporaryApproverRepository: TemporaryApproverRepository,
        authRepository: AuthRepository,
        @ApplicationContext context: Context
    ): ProjectRepository {
        return ProjectRepository(firestore, temporaryApproverRepository, authRepository, context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationRepository(
        firestore: FirebaseFirestore
    ): NotificationRepository {
        return NotificationRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideAuthRepository(firestore: FirebaseFirestore): AuthRepository {
        return AuthRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideFCMPushNotificationService(
        firestore: FirebaseFirestore,
        messaging: FirebaseMessaging,
        functions: FirebaseFunctions
    ): FCMPushNotificationService {
        return FCMPushNotificationService(firestore, messaging, functions)
    }
    
    @Provides
    @Singleton
    fun provideNotificationService(
        notificationRepository: NotificationRepository,
        authRepository: AuthRepository,
        projectRepository: ProjectRepository,
        firestore: FirebaseFirestore,
        fcmPushNotificationService: FCMPushNotificationService
    ): NotificationService {
        return NotificationService(notificationRepository, authRepository, projectRepository, firestore, fcmPushNotificationService)
    }
    
    @Provides
    @Singleton
    fun provideExportRepository(@ApplicationContext context: Context): ExportRepository {
        return ExportRepository(context, provideProfessionalReportGenerator(context))
    }
    
    @Provides
    @Singleton
    fun provideProfessionalReportGenerator(@ApplicationContext context: Context): ProfessionalReportGenerator {
        return ProfessionalReportGenerator(context)
    }
    
    @Provides
    @Singleton
    fun provideProfessionalExportRepository(
        @ApplicationContext context: Context,
        professionalReportGenerator: ProfessionalReportGenerator
    ): ProfessionalExportRepository {
        return ProfessionalExportRepository(context, professionalReportGenerator)
    }
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideDelegationExpiryService(
        firestore: FirebaseFirestore,
        projectRepository: ProjectRepository,
        temporaryApproverRepository: TemporaryApproverRepository,
        notificationService: NotificationService
    ): DelegationExpiryService {
        return DelegationExpiryService(firestore, projectRepository, temporaryApproverRepository, notificationService)
    }
    
    @Provides
    @Singleton
    fun provideDelegationScheduler(
        @ApplicationContext context: Context,
        workManager: WorkManager
    ): DelegationScheduler {
        return DelegationScheduler(context, workManager)
    }
    
    // ViewModels are automatically injected by Hilt, but we can provide additional dependencies here if needed
} 