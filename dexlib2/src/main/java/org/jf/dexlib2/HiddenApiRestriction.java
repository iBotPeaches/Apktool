/*
 * Copyright 2020, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public enum HiddenApiRestriction {
    WHITELIST(0, "whitelist", false),
    GREYLIST(1, "greylist", false),
    BLACKLIST(2, "blacklist", false),
    GREYLIST_MAX_O(3, "greylist-max-o", false),
    GREYLIST_MAX_P(4, "greylist-max-p", false),
    GREYLIST_MAX_Q(5, "greylist-max-q", false),
    CORE_PLATFORM_API(8, "core-platform-api", true),
    TEST_API(16, "test-api", true);

    private static final HiddenApiRestriction[] hiddenApiFlags = new HiddenApiRestriction[] {
            WHITELIST,
            GREYLIST,
            BLACKLIST,
            GREYLIST_MAX_O,
            GREYLIST_MAX_P,
            GREYLIST_MAX_Q
    };

    private static final HiddenApiRestriction[] domainSpecificApiFlags = new HiddenApiRestriction[] {
            CORE_PLATFORM_API,
            TEST_API
    };

    private static final Map<String, HiddenApiRestriction> hiddenApiRestrictionsByName;

    static {
        hiddenApiRestrictionsByName = new HashMap<>();
        for (HiddenApiRestriction hiddenApiRestriction : HiddenApiRestriction.values()) {
            hiddenApiRestrictionsByName.put(hiddenApiRestriction.toString(), hiddenApiRestriction);
        }
    }

    private static final int HIDDENAPI_FLAG_MASK = 0x7;

    private final int value;
    private final String name;
    private final boolean isDomainSpecificApiFlag;

    HiddenApiRestriction(int value, String name, boolean isDomainSpecificApiFlag) {
        this.value = value;
        this.name = name;
        this.isDomainSpecificApiFlag = isDomainSpecificApiFlag;
    }

    public String toString() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public boolean isSet(int value) {
        if (isDomainSpecificApiFlag) {
            return (value & this.value) != 0;
        } else {
            return (value & HIDDENAPI_FLAG_MASK) == this.value;
        }
    }

    public boolean isDomainSpecificApiFlag() {
        return isDomainSpecificApiFlag;
    }

    public static Set<HiddenApiRestriction> getAllFlags(int value) {
        HiddenApiRestriction normalRestriction = hiddenApiFlags[value & HIDDENAPI_FLAG_MASK];

        int domainSpecificPart = (value & ~HIDDENAPI_FLAG_MASK);
        if (domainSpecificPart == 0) {
            return ImmutableSet.of(normalRestriction);
        }
        Builder<HiddenApiRestriction> builder = ImmutableSet.builder();
        builder.add(normalRestriction);
        for (HiddenApiRestriction domainSpecificApiFlag : domainSpecificApiFlags) {
            if (domainSpecificApiFlag.isSet(value)) {
                builder.add(domainSpecificApiFlag);
            }
        }
        return builder.build();
    }

    public static String formatHiddenRestrictions(int value) {
        StringJoiner joiner = new StringJoiner("|");
        for (HiddenApiRestriction hiddenApiRestriction : getAllFlags(value)) {
            joiner.add(hiddenApiRestriction.toString());
        }
        return joiner.toString();
    }

    public static int combineFlags(Iterable<HiddenApiRestriction> flags) {
        boolean gotHiddenApiFlag = false;

        int value = 0;

        for (HiddenApiRestriction flag : flags) {
            if (flag.isDomainSpecificApiFlag) {
                value += flag.value;
            } else {
                if (gotHiddenApiFlag) {
                    throw new IllegalArgumentException(
                            "Cannot combine multiple flags for hidden api restrictions");
                }
                gotHiddenApiFlag = true;
                value += flag.value;
            }
        }

        return value;
    }

    public static HiddenApiRestriction forName(String name) {
        return hiddenApiRestrictionsByName.get(name);
    }
}
