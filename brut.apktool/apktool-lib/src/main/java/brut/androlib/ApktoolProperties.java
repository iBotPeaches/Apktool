/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public final class ApktoolProperties extends Properties {
    private static final Logger LOGGER = Logger.getLogger(ApktoolProperties.class.getName());

    private static volatile ApktoolProperties sInstance;

    private static String get(String key, String defaultValue) {
        if (sInstance == null) {
            sInstance = new ApktoolProperties();
        }
        return sInstance.getProperty(key, defaultValue);
    }

    public static String getVersion() {
        return get("application.version", "(unknown)");
    }

    public static String getSmaliVersion() {
        return get("smali.version", "(unknown)");
    }

    public static String getBaksmaliVersion() {
        return get("baksmali.version", "(unknown)");
    }

    private ApktoolProperties() {
        load(this, getClass(), "/apktool.properties");

        Properties smaliProps = new Properties();
        load(smaliProps, com.android.tools.smali.smali.Main.class, "/smali.properties");
        String smaliVersion = smaliProps.getProperty("application.version", "");
        if (!smaliVersion.isEmpty()) {
            put("smali.version", smaliVersion);
        }

        Properties baksmaliProps = new Properties();
        load(baksmaliProps, com.android.tools.smali.baksmali.Main.class, "/baksmali.properties");
        String baksmaliVersion = baksmaliProps.getProperty("application.version", "");
        if (!baksmaliVersion.isEmpty()) {
            put("baksmali.version", baksmaliVersion);
        }
    }

    private static void load(Properties props, Class<?> clz, String name) {
        InputStream in = null;
        try {
            in = clz.getResourceAsStream(name);
            if (in == null) {
                throw new FileNotFoundException(name);
            }
            props.load(in);
        } catch (NoClassDefFoundError ex) {
            LOGGER.warning("Could not find " + clz.getName());
        } catch (IOException ex) {
            LOGGER.warning("Could not load " + name);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
