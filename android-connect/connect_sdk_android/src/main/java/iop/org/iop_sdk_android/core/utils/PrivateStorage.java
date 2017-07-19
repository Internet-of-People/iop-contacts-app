package iop.org.iop_sdk_android.core.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by mati on 14/11/16.
 */

public class PrivateStorage {

    private Context context;

    public PrivateStorage(Context context) {
        this.context = context;
    }

    public void getFile(String name,byte[] buff) {
        File file = context.getDir("priv",MODE_PRIVATE);
        File fileTemp = new File(file,name);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(fileTemp);
            fileInputStream.read(buff);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                // nothing..
            }
        }
    }

    /**
     *
     * @param name
     * @return
     */
    public File getFile(String name) {
        return new File(context.getDir("priv",MODE_PRIVATE),name);
    }


    public void saveFile(String name,byte[] buf) throws IOException {
        File file = context.getDir("priv",MODE_PRIVATE);
        File saveFile = new File(file,name);
        saveFile.mkdirs();
        if (saveFile.exists()){
            saveFile.delete();
        }
        saveFile.createNewFile();
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(saveFile);
            fileOutputStream.write(buf);
            fileOutputStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                // nothing..
            }
        }
    }

    public void savePrivObj(String name,Object obj){
        File file = context.getDir(name,MODE_PRIVATE);
        File privFile = new File(file,name);
        FileOutputStream fileOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(privFile);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                // nothing..
            }

            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
            } catch (IOException e) {
                // nothign..
            }
        }

    }


    public Object getPrivObj(String name) {
        File file = context.getDir(name,MODE_PRIVATE);
        File privFile = new File(file.getPath()+"/"+name);
        FileInputStream fileInputStream = null;
        ObjectInputStream objectInputStream = null;
        Object object = null;
        try {
            if (!privFile.exists()) return null;
            fileInputStream = new FileInputStream(privFile);
            objectInputStream = new ObjectInputStream(fileInputStream);
            object = objectInputStream.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                // nothing
            }
            try {
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (IOException e) {
                // nothing
            }

        }
        return object;
    }
}
