plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.yidong222"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.yidong222"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 添加MultiDex支持
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // 启用 core library desugaring
        isCoreLibraryDesugaringEnabled = true
    }
    
    // 添加打包选项，解决依赖冲突
    packaging {
        resources {
            // 排除冲突文件
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/license.txt")
            excludes.add("META-INF/NOTICE")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/notice.txt")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/*.kotlin_module")
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/io.netty.versions.properties")
            excludes.add("META-INF/MANIFEST.MF")
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("de.hdodenhof:circleimageview:3.1.0")
    
    // MultiDex支持
    implementation("androidx.multidex:multidex:2.0.1")
    
    // Core library desugaring (支持Java 8+ API)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // SwipeRefreshLayout (下拉刷新)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Room 数据库
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    
    // MySQL JDBC (用于直接连接MySQL)
    implementation("mysql:mysql-connector-java:8.0.28")
    
    // Retrofit (用于API请求)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Gson (JSON解析)
    implementation("com.google.code.gson:gson:2.10.1")
    
    // OkHttp (网络请求)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Apache POI (Excel文件读写) - 使用较低版本，更兼容Android
    implementation("org.apache.poi:poi:4.1.2")
    implementation("org.apache.poi:poi-ooxml:4.1.2")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}