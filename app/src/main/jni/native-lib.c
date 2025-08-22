#include <jni.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <string.h>
#include <sys/system_properties.h>

#ifndef PROP_VALUE_MAX
#define PROP_VALUE_MAX 92
#endif

// Check for SU binary in various locations
JNIEXPORT jboolean JNICALL
Java_com_islab_rootbeer_data_datasource_NativeRootDetectionDataSource_checkSuBinary(JNIEnv *env, jobject thiz) {
    const char* su_paths[] = {
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/magisk/.core/bin/su",
        "/system/usr/we-need-root/su-backup",
        "/system/xbin/mu",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    };
    
    int path_count = sizeof(su_paths) / sizeof(su_paths[0]);
    
    for (int i = 0; i < path_count; i++) {
        FILE *file = fopen(su_paths[i], "r");
        if (file != NULL) {
            fclose(file);
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

// Check for root management apps
JNIEXPORT jboolean JNICALL
Java_com_islab_rootbeer_data_datasource_NativeRootDetectionDataSource_checkRootApps(JNIEnv *env, jobject thiz) {
    const char* root_apps[] = {
        "/data/data/com.noshufou.android.su",
        "/data/data/com.thirdparty.superuser",
        "/data/data/eu.chainfire.supersu",
        "/data/data/com.koushikdutta.superuser",
        "/data/data/com.zachspong.temprootremovejb",
        "/data/data/com.ramdroid.appquarantine",
        "/data/data/com.topjohnwu.magisk"
    };
    
    int app_count = sizeof(root_apps) / sizeof(root_apps[0]);
    
    for (int i = 0; i < app_count; i++) {
        struct stat sb;
        if (stat(root_apps[i], &sb) == 0) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

// Check for dangerous properties
JNIEXPORT jboolean JNICALL
Java_com_islab_rootbeer_data_datasource_NativeRootDetectionDataSource_checkDangerousProps(JNIEnv *env, jobject thiz) {
    // Check if we can access /system in read-write mode
    FILE *file = fopen("/system", "w");
    if (file != NULL) {
        fclose(file);
        return JNI_TRUE;
    }
    
    // Check for test-keys (indicates custom ROM)
    char prop_value[PROP_VALUE_MAX];
    if (__system_property_get("ro.build.tags", prop_value) > 0) {
        if (strcmp(prop_value, "test-keys") == 0) {
            return JNI_TRUE;
        }
    }
    
    // Check for debuggable build
    if (__system_property_get("ro.debuggable", prop_value) > 0) {
        if (strcmp(prop_value, "1") == 0) {
            return JNI_TRUE;
        }
    }
    
    return JNI_FALSE;
}