package com.oasisfeng.smartdialer.chinese;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.util.Log;

import com.android.providers.contacts.HanziToPinyin;
import com.android.providers.contacts.HanziToPinyin.Token;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/** @author Oasis */
public class SmartDialXposed implements IXposedHookLoadPackage {

    protected static final String TAG = "SmartDial.T9";

    @Override public void handleLoadPackage(final LoadPackageParam loadpkg) throws Throwable {
        if (! loadpkg.packageName.equals("com.android.dialer") && ! loadpkg.packageName.equals("com.google.android.dialer")) return;
        Log.i(TAG, "Patching Dialer app...");
        // com.android.dialer.database.DialerDatabaseHelper.insertNamePrefixes(SQLiteDatabase, Cursor) */
//        XposedHelpers.findAndHookMethod("com.android.dialer.database.DialerDatabaseHelper", loadpkg.classLoader,
//                "insertNamePrefixes", SQLiteDatabase.class, Cursor.class, new XC_MethodHook() {
//            @Override protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
//                final Cursor cursor = (Cursor) param.args[1];
//                if (cursor == null) return;
//                final CursorWrapper wrapper = new CursorWrapper(cursor) {
//
//                    @Override public int getColumnIndex(final String columnName) {
//                        if ("display_name".equals(columnName)) {
//                            Log.v(TAG, "display_name => lookup_key");
//                            return super.getColumnIndex("lookup_key");
//                        }
//                        return super.getColumnIndex(columnName);
//                    }
//
//                    @Override public String getString(final int columnIndex) {
//                        final String value = super.getString(columnIndex);
//                        Log.v(TAG, "lookup_key: " + value);
//                        return value;
//                    }
//                };
//                param.args[1] = wrapper;
//            }
//        });

        // public static ArrayList<String> com.android.dialer.dialpad.SmartDialPrefix.generateNamePrefixes(String index)
        XposedHelpers.findAndHookMethod("com.android.dialer.dialpad.SmartDialPrefix", loadpkg.classLoader,
                "generateNamePrefixes", String.class, new XC_MethodHook() {
            // Convert index into Pin Yin.
            @Override protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final String name = (String) param.args[0];
                if (name == null) return;
                final List<Token> tokens = HanziToPinyin.getInstance().get(name);
                if (tokens.isEmpty()) return;
                final StringBuilder buffer = new StringBuilder();
                for (final Token token : tokens)
                    buffer.append(' ').append(token.target);
                final String full = buffer.substring(1).toLowerCase(Locale.US);
                Log.d(TAG, name + ":" + full);
                param.args[0] = full;
            }
        });

        // boolean com.android.dialer.dialpad.SmartDialNameMatcher
        //	.matchesCombination(String displayName, String query, ArrayList<SmartDialMatchPosition> matchList)
        XposedHelpers.findAndHookMethod("com.android.dialer.dialpad.SmartDialNameMatcher", loadpkg.classLoader,
                "matchesCombination", String.class, String.class, ArrayList.class, new XC_MethodHook() {

            @Override protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final String name = (String) param.args[0];
                if (name == null) return;
                final List<Token> tokens = HanziToPinyin.getInstance().get(name);
                if (tokens.isEmpty()) return;
                final StringBuilder full = new StringBuilder();
                for (final Token token : tokens)
                    full.append(' ').append(token.target);
                param.args[0] = full.substring(1).toLowerCase(Locale.US);
            }

            @Override protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final ArrayList<?> pos_list = (ArrayList<?>) param.args[2];
                pos_list.clear();
                Log.d(TAG, param.args[1] + " ? " + param.args[0] + " = " + param.getResult());
            }
        });
    }
}
