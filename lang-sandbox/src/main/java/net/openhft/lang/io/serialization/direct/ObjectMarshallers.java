/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.lang.io.serialization.direct;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static net.openhft.lang.io.serialization.direct.DirectSerializationMetadata.SerializationMetadata;

final class ObjectMarshallers {
    private static final Logger Log = Logger.getLogger(ObjectMarshallers.class.getName());

    private static final Map<Class, ObjectMarshaller> metadata = new HashMap<Class, ObjectMarshaller>();

    @SuppressWarnings("unchecked")
    public static <T> ObjectMarshaller<T> forClass(Class<T> clazz) {
        ObjectMarshaller om = metadata.get(clazz);
        if (om == null) {
            List<Field> fields = Introspect.fields(clazz);
            List<Field> eligibleFields = DirectSerializationFilter.stopAtFirstIneligibleField(fields);

            SerializationMetadata serializationMetadata;

            if (hasIneligibleFields(fields, eligibleFields)) {
                WarnAboutIneligibleFields.apply(clazz, fields, eligibleFields);
                serializationMetadata = DirectSerializationMetadata.extractMetadataForPartialCopy(eligibleFields);
            } else {
                serializationMetadata = DirectSerializationMetadata.extractMetadata(eligibleFields);
            }

            om = new ObjectMarshaller<T>(serializationMetadata);
            Log.log(WARNING, String.format("Class %s has metadata %s", clazz.getName(), serializationMetadata));
            metadata.put(clazz, om);
        }

        return (ObjectMarshaller<T>) om;
    }

    private static boolean hasIneligibleFields(List<Field> allFields, List<Field> eligibleFields) {
        return allFields.size() != eligibleFields.size();
    }

    private static class WarnAboutIneligibleFields {
        static void apply(Class clazz, List<Field> allFields, List<Field> eligibleFields) {
            List<Field> ineligibleFields = allFields.subList(eligibleFields.size(), allFields.size());
            Log.log(WARNING, String.format(
                    "The following fields in Class %s will not be copied by ObjectMarshaller:\n%s",
                    clazz.getName(),
                    commaSeparate(ineligibleFields)
            ));
        }

        private static String commaSeparate(Collection<Field> fields) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Field field : fields) {
                if (first) {
                    sb.append('\t');
                    sb.append(field.getName());
                    first = false;
                } else {
                    sb.append("\n\t");
                    sb.append(field.getName());
                }
            }

            return sb.toString();
        }
    }
}
