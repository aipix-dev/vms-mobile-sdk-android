plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	id("kotlin-android")
	id("kotlin-parcelize")
	id("kotlin-kapt")
	id("maven-publish")
}

android {
	namespace = "com.mobile.vms"
	compileSdk = 34
	lint {
		checkReleaseBuilds = false
	}
	if (project.hasProperty("devBuild")) {
		splits.abi.isEnable = false
		splits.density.isEnable = false
		aaptOptions.cruncherEnabled = false
	}
	defaultConfig {
		minSdk = 28
		buildToolsVersion = "34.0.0"
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		buildConfigField("String", "VERSION_NAME", "\"23.09.0.0\"")
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
	}
	kotlinOptions {
		jvmTarget = "17"
	}
	buildFeatures {
		dataBinding = true
		viewBinding = true
		buildConfig = true
	}
	composeOptions {
		kotlinCompilerExtensionVersion = "1.4.3"
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

dependencies {
	implementation("androidx.core:core-ktx:1.10.1")
	implementation("com.google.android.material:material:1.9.0")
	implementation("androidx.appcompat:appcompat:1.6.1")
	testImplementation("junit:junit:4.13.2")

	implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
	implementation("io.reactivex.rxjava2:rxjava:2.2.11")

	implementation("androidx.media3:media3-common:1.3.0")
	implementation("androidx.media3:media3-exoplayer:1.3.0")
	implementation("androidx.media3:media3-exoplayer-hls:1.3.0")
	implementation("androidx.media3:media3-exoplayer-rtsp:1.3.0")

	api("com.squareup.retrofit2:retrofit:2.9.0")
	api("com.squareup.retrofit2:converter-moshi:2.4.0")
	api("com.squareup.retrofit2:converter-gson:2.7.1")
	api("com.squareup.retrofit2:adapter-rxjava2:2.6.2")
	api("com.squareup.retrofit2:converter-scalars:2.9.0")
	api("com.squareup.okhttp3:logging-interceptor:4.9.0")
	api("com.pusher:pusher-java-client:2.0.2")
}
