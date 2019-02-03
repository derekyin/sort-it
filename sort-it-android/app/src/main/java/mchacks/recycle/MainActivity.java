package mchacks.recycle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d("", "dispatchTakePictureIntent: " + ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bitmap bmp = BitmapFactory.decodeFile(mCurrentPhotoPath);
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bmp);
                final FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                        .getCloudImageLabeler();
                labeler.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                classify(labels);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                alert("Image cannot be recognized.");
                            }
                        });

            }
        }
    }

    private void classify(List<FirebaseVisionImageLabel> labels) {
//        TextView text = findViewById(R.id.text);
//        text.setText("");
        Classification c = new Classification();
        for (int i = 0; i < labels.size(); i++) {
            String thing = labels.get(i).getText();
//            text.append(thing + "\n");
            Log.d("Label", thing);
            c = lookup(thing);
            if(c.result){
                break;
            }
        }

        if(c.result){
            showResult(c, "");
        }
        else{
            showResult(c, "Not recognized.");
        }
    }

    private Classification lookup(String thing){
        Log.d("plural", English.plural(thing.toLowerCase()));

        Classification c = new Classification();

        InputStream ins = getResources().openRawResource(
                getResources().getIdentifier("classification",
                        "raw", getPackageName()));
        BufferedReader r = new BufferedReader(new InputStreamReader(ins));
//        StringBuilder total = new StringBuilder();
        ArrayList<String> lines = new ArrayList<String>();

        try {
            for (String line; (line = r.readLine()) != null; ) {
                lines.add(line);
            }
        }
        catch(IOException ex){
            Log.e("", ex.toString());
        }

        for(int i = 0; i < lines.size(); i++){
            String[] s = lines.get(i).split(",");
            for(int j = 0; j < s.length; j++){
                if(s[j].toLowerCase().trim().equals(thing.toLowerCase().trim())){
                    c.result = true;
                    c.bin = lines.get(i).split(",")[0];
                    c.category = lines.get(i);
                    break;
                }
            }
            if(c.result){
                break;
            }
        }

        if(!c.result){
            for(int i = 0; i < lines.size(); i++){
                String[] s = lines.get(i).split(",");
                for(int j = 0; j < s.length; j++){
                    if(s[j].toLowerCase().trim().contains(thing.toLowerCase().trim())){
                        c.result = true;
                        c.bin = lines.get(i).split(",")[0];
                        c.category = lines.get(i);
                        break;
                    }
                }
                if(c.result){
                    break;
                }
            }
        }

        return  c;
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void alert(String message) {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle("Recycling");
        dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //dismiss the blue
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    private void showResult(Classification c, String err){
        AlertDialog.Builder alertadd = new AlertDialog.Builder(this);
        alertadd.setTitle(c.category);
        if(err.length() != 0){
            alertadd.setTitle(err);
        }
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.garbage, null);
        if(c.bin.equals("Blue Bin")){
            view = factory.inflate(R.layout.blue, null);
        }
        else if(c.bin.equals("Green Bin")){
            view = factory.inflate(R.layout.green, null);
        }
        alertadd.setView(view);
        alertadd.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int sumthin) {

            }
        });

        alertadd.show();
    }

}


