import java.io.*;
 
public class FilesApp {
 
    public static void main(String[] args) {
         
        String text = "0000111100001111"; // ������ ��� ������
        try(FileOutputStream fos=new FileOutputStream("notes.txt"))
        {
            // ������� ������ � �����
            byte[] buffer = text.getBytes();
             
            fos.write(buffer, 0, buffer.length);
        }
        catch(IOException ex){
             
            System.out.println(ex.getMessage());
        } 
    } 
}