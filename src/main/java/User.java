import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;

class User{
    long id;
    byte[] key;

    public User(int id, byte[] key) {
        this.id = id;
        this.key = key;
    }

    public String toString() {
        return id + ":" + Arrays.toString(key);
    }
}

class SuperEncoder {

    private static final Unsafe unsafe;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long idLongOffset;
    private long keyByteArrayOffset;
    private Class aClass;

    public SuperEncoder(){
        try {
            aClass = User.class;
            idLongOffset = unsafe.objectFieldOffset(User.class.getDeclaredField("id"));
            keyByteArrayOffset = unsafe.objectFieldOffset(User.class.getDeclaredField("key"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid schema");
        }
    }

    public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(bos);

        os.writeLong(unsafe.getLong(obj, idLongOffset));

        byte[] array = (byte[]) unsafe.getObject(obj, keyByteArrayOffset);
        os.writeInt(array.length);
        os.write(array);

        return bos.toByteArray();
    }

    public Object deserialize(byte[] stream) throws InstantiationException, IOException {
        Object obj = unsafe.allocateInstance(aClass);

        DataInputStream is = new DataInputStream(new ByteArrayInputStream(stream));

        unsafe.putLong(obj, idLongOffset, is.readLong());

        int size = is.readInt();
        byte[] array = new byte[size];
        is.read(array, 0, size);
        unsafe.putObject(obj, keyByteArrayOffset, array);

        return obj;
    }

    public static void main(String[] args) throws Exception{
        SuperEncoder superEncoder = new SuperEncoder();
        Object obj = new User(1, new byte[]{0,1,2,3,4,5});
        byte[] stream = superEncoder.serialize(obj);
        System.out.println(superEncoder.deserialize(stream));
    }
}  