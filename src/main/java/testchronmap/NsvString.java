package testchronmap;

final public class NsvString {
    final static char FLAT_DEL = '\4';
    final static char FLAT_NULL = '\0';

    public static StringBuilder joinTo(StringBuilder bld, int[] ray) {
        if (ray.length == 0)
            // we should arguably return a null string here but StringBuilder cannot be overridden with null
            // better to have caller fail-fast and not use the routine when there is no data at all like this
            throw new RuntimeException("Cannot join an empty array  - cannot encode an empty array into a String");

        for (int i = 0; i < ray.length - 1; i++) {
            bld.append(ray[i]).append(FLAT_DEL);
        }
        int last = ray.length - 1;
        bld.append(ray[last]);
        return bld;
    }

    public static StringBuilder joinTo(StringBuilder bld, Object[] ray) {
        if (ray.length == 0)
            // we should arguably return a null string here but StringBuilder cannot be overridden with null
            // better to have caller fail-fast and not use the routine when there is no data at all like this
            throw new RuntimeException("Cannot join an empty array  - cannot encode an empty array into a String");

        for (int i = 0; i < ray.length - 1; i++) {
            if (ray[i] == null)
                bld.append(FLAT_NULL).append(FLAT_DEL);
            else
                bld.append(ray[i]).append(FLAT_DEL);
        }
        int last = ray.length - 1;
        if (ray[last] == null)
            bld.append(FLAT_NULL);
        else
            bld.append(ray[last]);
        return bld;
    }

    public static StringBuilder joinTo(StringBuilder bld, CharSequence[] ray) {
        if (ray.length == 0)
            // we should arguably return a null string here but StringBuilder cannot be overridden with null
            // better to have caller fail-fast and not use the routine when there is no data at all like this
            throw new RuntimeException("Cannot join an empty array  - cannot encode an empty array into a String");

        for (int i = 0; i < ray.length - 1; i++) {
            if (ray[i] == null)
                bld.append(FLAT_NULL).append(FLAT_DEL);
            else
                bld.append(ray[i]).append(FLAT_DEL);
        }
        int last = ray.length - 1;
        if (ray[last] == null)
            bld.append(FLAT_NULL);
        else
            bld.append(ray[last]);
        return bld;
    }

    public static String[] splitTo(StringBuilder tmpbuf, String s) {
        if (s.length() == 0)
            return new String[]{""};

        int len = 1;  // note lat item does not have a delimiter
        for (var c : s.toCharArray()) {
            if (c == FLAT_DEL)
                len++;
        }

        var ray = new String[len];

        int slot = 0;
        tmpbuf.setLength(0);
        var lastc = ' ';
        for (char c : s.toCharArray()) {
            if (c > '\5')
                tmpbuf.append(c);
            else {
                if (c == FLAT_DEL)
                    if (lastc == FLAT_NULL)
                        ray[slot++] = null;
                    else
                        ray[slot++] = tmpbuf.toString();
                tmpbuf.setLength(0);
            }
            lastc = c;
        }
        if (lastc == FLAT_NULL)
            ray[slot] = null;
        else
            ray[slot] = tmpbuf.toString();
        return ray;
    }

}
