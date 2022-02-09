package io.oreto.jackson;

import java.util.*;

class MultiMap<K, V> {
    private final Map<K, List<V>> map = new HashMap<>();

    final MultiMap<K, V> put(K k, V v) {
        if ((v instanceof String && !"".equals(v)) || (!(v instanceof String) && v != null)) {
            if (map.containsKey(k)) map.get(k).add(v);
            else map.put(k, new ArrayList<V>() {{
                add(v);
            }});
        }
        return this;
    }

    /**
     * Sort each list in the map
     * @param c The comparator used by sort.
     * @return A self referencing MultiMap to support a fluent api.
     */
    MultiMap<K, V> sort(Comparator<? super V> c) {
        map.forEach(((k, vs) -> vs.sort(c)));
        return this;
    }

    Map<K, List<V>> asMap() { return map; }
}
