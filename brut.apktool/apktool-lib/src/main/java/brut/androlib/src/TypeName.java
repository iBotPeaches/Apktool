/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package brut.androlib.src;

import brut.androlib.AndrolibException;
import brut.util.Duo;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class TypeName {
	public final String package_;
	public final String type;
	public final String innerType;
	public final int array;

	public TypeName(String type, int array) {
		this(null, type, null, array);
	}

	public TypeName(String package_, String type, String innerType, int array) {
		this.package_ = package_;
		this.type = type;
		this.innerType = innerType;
		this.array = array;
	}

	public String getShortenedName() {
		return getName("java.lang".equals(package_), isFileOwner());
	}

	public String getName() {
		return getName(false, false);
	}

	public String getName(boolean excludePackage, boolean separateInner) {
		String name = (package_ == null || excludePackage ? "" : package_ + '.')
				+ type
				+ (innerType != null ? (separateInner ? '$' : '.') + innerType
						: "");
		for (int i = 0; i < array; i++) {
			name += "[]";
		}
		return name;
	}

	public String getJavaFilePath() {
		return getFilePath(isFileOwner()) + ".java";
	}

	public String getSmaliFilePath() {
		return getFilePath(true) + ".smali";
	}

	public String getFilePath(boolean separateInner) {
		return package_.replace('.', File.separatorChar) + File.separatorChar
				+ type + (separateInner && isInner() ? "$" + innerType : "");
	}

	public boolean isInner() {
		return innerType != null;
	}

	public boolean isArray() {
		return array != 0;
	}

	public boolean isFileOwner() {
		if (mIsFileOwner == null) {
			mIsFileOwner = true;
			if (isInner()) {
				char c = innerType.charAt(0);
				if (c < '0' || c > '9') {
					mIsFileOwner = false;
				}
			}
		}
		return mIsFileOwner;
	}

	@Override
	public String toString() {
		return getName();
	}

	public static TypeName fromInternalName(String internal)
			throws AndrolibException {
		Duo<TypeName, Integer> duo = fetchFromInternalName(internal);
		if (duo.m2 != internal.length()) {
			throw new AndrolibException("Invalid internal name: " + internal);
		}
		return duo.m1;
	}

	public static List<TypeName> listFromInternalName(String internal)
			throws AndrolibException {
		List<TypeName> types = new ArrayList<TypeName>();
		while (!internal.isEmpty()) {
			Duo<TypeName, Integer> duo = fetchFromInternalName(internal);
			types.add(duo.m1);
			internal = internal.substring(duo.m2);
		}
		return types;
	}

	public static Duo<TypeName, Integer> fetchFromInternalName(String internal)
			throws AndrolibException {
		String origInternal = internal;
		int array = 0;

		boolean isArray = false;
		do {
			if (internal.isEmpty()) {
				throw new AndrolibException("Invalid internal name: "
						+ origInternal);
			}
			isArray = internal.charAt(0) == '[';
			if (isArray) {
				array++;
				internal = internal.substring(1);
			}
		} while (isArray);

		int length = array + 1;
		String package_ = null;
		String type = null;
		String innerType = null;
		switch (internal.charAt(0)) {
		case 'B':
			type = "byte";
			break;
		case 'C':
			type = "char";
			break;
		case 'D':
			type = "double";
			break;
		case 'F':
			type = "float";
			break;
		case 'I':
			type = "int";
			break;
		case 'J':
			type = "long";
			break;
		case 'S':
			type = "short";
			break;
		case 'Z':
			type = "boolean";
			break;
		case 'V':
			type = "void";
			break;
		case 'L':
			int pos = internal.indexOf(';');
			if (pos == -1) {
				throw new AndrolibException("Invalid internal name: "
						+ origInternal);
			}
			length += pos;
			internal = internal.substring(1, pos);

			pos = internal.lastIndexOf('/');
			if (pos == -1) {
				package_ = "";
				type = internal;
			} else {
				package_ = internal.substring(0, pos).replace('/', '.');
				type = internal.substring(pos + 1);
			}

			pos = type.indexOf('$');
			if (pos != -1) {
				innerType = type.substring(pos + 1);
				type = type.substring(0, pos);
			}
			break;
		default:
			throw new AndrolibException("Invalid internal name: "
					+ origInternal);
		}

		return new Duo<TypeName, Integer>(new TypeName(package_, type,
				innerType, array), length);
	}

	private Boolean mIsFileOwner;
}
