package tvshows_renamer;

import java.io.*;
import java.util.*;

public class RAR_pass {

    public static void main(String[] args) throws Exception {
        testARar("C:\\TEST\\RAR\\testNoPass.rar");
        testARar("C:\\TEST\\RAR\\testHide.rar");
        testARar("C:\\TEST\\RAR\\test.rar");
    }

    public static void testARar(String filename){
        byte[] file = getRarFile(filename);
        if(!checkIfRAR(file)){
            System.out.println("Error: The file " + filename + " is not a valid RAR archive.");
            return ;
        }

        extractFullHeader(file);
    }

    public static Header_Block[] Marker_Block = new Header_Block[] {
        new Header_Block("HEAD_CRC", 2),
        new Header_Block("HEAD_TYPE", 1),   //0x72
        new Header_Block("HEAD_FLAGS", 2),
        new Header_Block("HEAD_SIZE", 2)
        //new Header_Block("ADD_SIZE", 4, "HEAD_FLAGS", 0x8000)
    };

    public static Header_Block[] Archive_Header = new Header_Block[] {
        new Header_Block("HEAD_CRC", 2),
        new Header_Block("HEAD_TYPE", 1),   //0x73
        new Header_Block("HEAD_FLAGS", 2),
        new Header_Block("HEAD_SIZE", 2),
        new Header_Block("RESERVED1", 2),
        new Header_Block("RESERVED2", 4)
    };

    public static Header_Block[] File_Header = new Header_Block[] {
        new Header_Block("HEAD_CRC", 2),
        new Header_Block("HEAD_TYPE", 1),   //0x74
        new Header_Block("HEAD_FLAGS", 2),
        new Header_Block("HEAD_SIZE", 2),
        new Header_Block("PACK_SIZE", 4),
        new Header_Block("UNP_SIZE", 4),
        new Header_Block("HOST_OS", 1),
        new Header_Block("FILE_CRC", 4),
        new Header_Block("FTIME", 4),
        new Header_Block("UNP_VER", 1),
        new Header_Block("METHOD", 1),
        new Header_Block("NAME_SIZE", 2),
        new Header_Block("ATTR", 4),
        new Header_Block("HIGH_PACK_SIZE", 4, "HEAD_FLAGS", 0x100),
        new Header_Block("HIGH_UNP_SIZE", 4, "HEAD_FLAGS", 0x100),
        new Header_Block("FILE_NAME", "NAME_SIZE"),
        new Header_Block("SALT", 1, "HEAD_FLAGS", 0x400)
        //new Header_Block("EXT_TIME", 4, "HEAD_FLAGS", 0x1000),
    };

    public static Header_Block[][] Header_Type = new Header_Block[][]
        {Marker_Block, Archive_Header, File_Header};


    public static List<String> listOfString = new ArrayList<String>();
    public static TreeMap<String, Long> extractBlock_ext(byte[] data, int start, int blockType){
        TreeMap<String, Long> header = new TreeMap<String, Long>();
        int pos = start;
        for(Header_Block head : Header_Type[blockType])
            if( !head.optionnal || (head.optionnal &&
                        doesContain(header.get(head.relationName),head.relationByte)))
            {
                int nbytes;
                if( head.fixedNBYTES ){
                    nbytes = (int)(long)header.get(head.relationNBYTES);
                    header.put(head.name, (long)listOfString.size());
                    listOfString.add(getString(data, pos, nbytes));
                }
                else{
                    nbytes = head.nbytes;
                    header.put(head.name, getLong(data, pos, nbytes));
                }
                pos += nbytes;
            }
            else
                header.put(head.name, null);
        header.put("EndPos", (long)pos);
        return header;
    }

    public static boolean doesContain(long in, long toCheck){
        return ((in & toCheck) == toCheck);
    }

    public static TreeMap<String, Long> extractBlock(byte[] data, int start){
        long ltype = extractBlock_ext(data, start, 0).get("HEAD_TYPE");
        int type;
        if(ltype == 0x72) type = 0;
        else if(ltype == 0x73) type = 1;
        else if(ltype == 0x74) type = 2;
        else{
            System.out.println("Error in rar file, block type unknown.");
            return null;
        }
        return extractBlock_ext(data, start, type);
    }

    public static void extractFullHeader(byte[] data){
        int pos;

        TreeMap<String, Long> Marker_ = extractBlock(data, 0);
        pos = (int)(long)Marker_.get("EndPos");
        TreeMap<String, Long> Archive_ = extractBlock(data, pos);

        int passType = 0;
        if(doesContain(Archive_.get("HEAD_FLAGS"), 0x0080))
            passType = 1;
        else {
            pos = (int)(long)Archive_.get("EndPos");
            TreeMap<String, Long> File_ = extractBlock(data, pos);
            if(doesContain(File_.get("HEAD_FLAGS"), 0x04))
                passType = 2;
        }

        System.out.println("Password type : " + passType);

        //printAllHeader(File_);
        //System.out.println("Filename : " + listOfString.get((int)(long)File_.get("FILE_NAME")));
        //System.out.println("Is password : " + (doesContain(File_.get("HEAD_FLAGS"),0x04)));
    }

    public static void printAllHeader(TreeMap<String, Long> map){
        for(String key : map.descendingKeySet()){
            if(map.get(key)!=null)
                System.out.println(key + " -> " + Long.toHexString(map.get(key)));
        }
    }

    public static long RAR_marker = 0x21726152L;
    public static boolean checkIfRAR(byte[] data){
        long marker_header = getLong(data, 0, 4);
        return (marker_header == RAR_marker);
    }

    public static byte[] getRarFile(String rarFile) {
        File file = new File(rarFile);
        int length = (int)file.length();
        byte[] data = new byte[length];
        
        try {
            FileInputStream FileStream = new FileInputStream(rarFile);
            BufferedInputStream FileBuffered = new BufferedInputStream(FileStream, 1);
            FileBuffered.read(data);
            FileBuffered.close();
        }
        catch (Exception e){
            System.out.println("Error: Impossible to open the rar file " + rarFile + " (" + e.toString() + ")");
        }
        
        return data;
    }

    public static long getLong(byte[] bytes, int start, int len) {
        long ret = 0;
        long mask = 0;
        int end = start + len - 1;

        if (start < 0 || end >= bytes.length) {
            return ret;
        }

        for (int i = start, j = 0; i <= end; i++, j++) {
            ret |= (bytes[i] & 0xFF) << (8 * j); // mask and shift left
            mask = (mask << 8) | 0xFF; // generate the final mask
        }

        return ret & mask;
    }

    public static String getString(byte[] bytes, int start, int len) {
        String ret = "";
        long mask = 0;
        int end = start + len - 1;

        if (start < 0 || end >= bytes.length) {
            return null;
        }

        for (int i = start ; i <= end ; i++)
            ret += (char)bytes[i];

        return ret;
    }

    public static int ByteToInt(byte ByteToCode) {
        int IntShot = (int) ByteToCode;
        if (IntShot >= 0)
            return IntShot;
        else 
            return 256 + IntShot;
    }
}

class Header_Block {
    public String name;
    public int nbytes;
    public boolean optionnal = false;
    public String relationName;
    public long relationByte;
    public boolean fixedNBYTES = false;
    public String relationNBYTES;

    public Header_Block(String name, int nbytes) {
        this.name = name;
        this.nbytes = nbytes;
    }

    public Header_Block(String name, String relationNBYTES){
        this.name = name;
        this.relationNBYTES = relationNBYTES;
        this.fixedNBYTES = true;
    }

    public Header_Block(String name, int nbytes, String relationName, long relationByte){
        this.name = name;
        this.nbytes = nbytes;
        this.relationName = relationName;
        this.relationByte = relationByte;
        this.optionnal = true;
    }
}
