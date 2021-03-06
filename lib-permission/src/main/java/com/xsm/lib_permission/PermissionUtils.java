package com.xsm.lib_permission;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.SimpleArrayMap;

import com.xsm.lib_permission.menu.DeviceHUAWEI;
import com.xsm.lib_permission.menu.DeviceMEIZU;
import com.xsm.lib_permission.menu.DeviceOPPO;
import com.xsm.lib_permission.menu.DeviceVIVO;
import com.xsm.lib_permission.menu.DeviceXIAOMI;
import com.xsm.lib_permission.menu.IDeviceSetting;
import com.xsm.lib_permission.menu.DeviceDefault;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Author: 夏胜明
 * Date: 2018/7/16 0016
 * Email: xiasem@163.com
 * Description:
 */
public class PermissionUtils {

    public static final int DEFAULT_REQUEST_CODE = 0xABC1994;

    private static SimpleArrayMap<String, Integer> MIN_SDK_PERMISSIONS;
    static {
        //存储android个别权限当前版本是否存在
        MIN_SDK_PERMISSIONS = new SimpleArrayMap<>(8);
        MIN_SDK_PERMISSIONS.put("com.android.voicemail.permission.ADD_VOICEMAIL", 14);
        MIN_SDK_PERMISSIONS.put("android.permission.BODY_SENSORS", 20);
        MIN_SDK_PERMISSIONS.put("android.permission.READ_CALL_LOG", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.READ_EXTERNAL_STORAGE", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.USE_SIP", 9);
        MIN_SDK_PERMISSIONS.put("android.permission.WRITE_CALL_LOG", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.SYSTEM_ALERT_WINDOW", 23);
        MIN_SDK_PERMISSIONS.put("android.permission.WRITE_SETTINGS", 23);
    }

    private static HashMap<String, Class<? extends IDeviceSetting>> permissionMenu = new HashMap<>();

    private static final String MANUFACTURER_DEFAULT = "Default";//默认
    public static final String MANUFACTURER_HUAWEI = "huawei";//华为
    public static final String MANUFACTURER_MEIZU = "meizu";//魅族
    public static final String MANUFACTURER_XIAOMI = "xiaomi";//小米
    public static final String MANUFACTURER_SONY = "sony";//索尼
    public static final String MANUFACTURER_OPPO = "oppo";
    public static final String MANUFACTURER_LG = "lg";
    public static final String MANUFACTURER_VIVO = "vivo";
    public static final String MANUFACTURER_SAMSUNG = "samsung";//三星
    public static final String MANUFACTURER_LETV = "letv";//乐视
    public static final String MANUFACTURER_ZTE = "zte";//中兴
    public static final String MANUFACTURER_YULONG = "yulong";//酷派
    public static final String MANUFACTURER_LENOVO = "lenovo";//联想

    static {
        permissionMenu.put(MANUFACTURER_DEFAULT, DeviceDefault.class);
        permissionMenu.put(MANUFACTURER_HUAWEI, DeviceHUAWEI.class);
        permissionMenu.put(MANUFACTURER_MEIZU, DeviceMEIZU.class);
        permissionMenu.put(MANUFACTURER_OPPO, DeviceOPPO.class);
        permissionMenu.put(MANUFACTURER_VIVO, DeviceVIVO.class);
        permissionMenu.put(MANUFACTURER_XIAOMI, DeviceXIAOMI.class);
    }

    /**
     * 检测是否需要请求权限
     * @param context
     * @param permissions
     * @return
     */
    public static boolean hasPermission(Context context, String... permissions) {
        for (String permission : permissions) {
            if (permissionExists(permission) && !hasSelfPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检测某个权限是否已经授权；如果已授权则返回true，如果未授权则返回false
     * @param context
     * @param permission
     * @return
     */
    private static boolean hasSelfPermission(Context context, String permission) {
        try {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        } catch (RuntimeException t) {
            return false;
        }
    }

    /**
     * 检测当前版本权限是否存在
     * @param permission
     * @return
     */
    private static boolean permissionExists(String permission) {
        Integer minVersion = MIN_SDK_PERMISSIONS.get(permission);
        return minVersion == null || Build.VERSION.SDK_INT >= minVersion;
    }

    /**
     * 验证权限是否申请成功
     * @param context
     * @param gantedResults
     * @return
     */
    public static boolean verifyPermission(Context context, int ... gantedResults) {
        if (gantedResults == null || gantedResults.length == 0 ) {
            return false;
        }
        for (int ganted : gantedResults) {
            if (ganted != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查需要给予的权限是否需要显示理由
     * 此方法主要用于给用户一个申请权限的解释，该方法只有在用户在上一次已经拒绝过你的这个权限申请。
     * 即用户已经拒绝一次了，你又弹个授权框，你需要给用户一个解释，为什么要授权，则使用该方法。
     * @param activity
     * @param permissions
     * @return
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    public static void invokAnnotation(Object object, Class annotationClass, int requestCode, ArrayList<String> refusedPermissions) {
        //获取切面上下文的类型
        Class<?> clz = object.getClass();
        //获取类型中的方法
        Method[] methods = clz.getDeclaredMethods();
        if (methods == null) {
            return;
        }
        for (Method method : methods) {
            //获取该方法是否有PermissionCanceled注解
            boolean isHasAnnotation = method.isAnnotationPresent(annotationClass);
            if (isHasAnnotation) {
                method.setAccessible(true);
                try {
                    methodInvoke(method, object, requestCode, refusedPermissions);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    private static void methodInvoke(Method method, Object object, int requestCode, ArrayList<String> refusedPermissions) throws InvocationTargetException, IllegalAccessException {
        Class<?>[] parameterClazz = method.getParameterTypes();
        if (parameterClazz == null) {
            method.invoke(object);
        }
        else if (parameterClazz.length == 0) {
            method.invoke(object);
        }
        else if (parameterClazz.length == 1) {
            String paramTypeName = parameterClazz[0].getName();
            if ("java.lang.Integer".equals(paramTypeName) || "int".equals(paramTypeName)){
                method.invoke(object, requestCode);
            } else if ("java.util.ArrayList".equals(paramTypeName) || "java.util.List".equals(paramTypeName)) {
                method.invoke(object, refusedPermissions);
            } else {
                method.invoke(object);
            }
        } else if (parameterClazz.length == 2) {
            String paramTypeName1 = parameterClazz[0].getName();
            String paramTypeName2 = parameterClazz[1].getName();

            if (("java.lang.Integer".equals(paramTypeName1) || "int".equals(paramTypeName1))
                    && ("java.util.ArrayList".equals(paramTypeName2) || "java.util.List".equals(paramTypeName2))) {
                method.invoke(object, requestCode, refusedPermissions);
            } else if (("java.lang.Integer".equals(paramTypeName2) || "int".equals(paramTypeName2))
                    && ("java.util.ArrayList".equals(paramTypeName1) || "java.util.List".equals(paramTypeName1))) {
                method.invoke(object, refusedPermissions, requestCode);
            } else {
                method.invoke(object);
            }
        } else {
            method.invoke(object);
        }

    }


    /**
     * 跳转到厂商权限设置页面
     * @param context
     */
    public static void goToDeviceSetting(Context context) {
        Class clazz = permissionMenu.get(Build.MANUFACTURER.toLowerCase());
        if (clazz == null) {
            clazz = permissionMenu.get(MANUFACTURER_DEFAULT);
        }

        try {
            IDeviceSetting iMenu = (IDeviceSetting) clazz.newInstance();
            iMenu.goToDeviceSetting(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
