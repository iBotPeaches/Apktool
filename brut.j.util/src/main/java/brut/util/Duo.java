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
package brut.util;

import java.util.Objects;

public class Duo<T1, T2> {
    public final T1 m1;
    public final T2 m2;

    public Duo(T1 t1, T2 t2) {
        m1 = t1;
        m2 = t2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Duo<T1, T2> other = (Duo<T1, T2>) obj;
        if (!Objects.equals(m1, other.m1)) {
            return false;
        }
        return Objects.equals(m2, other.m2);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + (m1 != null ? m1.hashCode() : 0);
        hash = 71 * hash + (m2 != null ? m2.hashCode() : 0);
        return hash;
    }
}
