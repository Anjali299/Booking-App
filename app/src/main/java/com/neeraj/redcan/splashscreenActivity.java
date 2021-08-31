 package com.neeraj.redcan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.neeraj.redcan.Model.DriverInfoModel;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.functions.Action;

import static java.util.concurrent.TimeUnit.SECONDS;

 public class  splashscreenActivity extends AppCompatActivity {

     private final static int LOGIN_REQUEST_CODE =7171;
     private List<AuthUI.IdpConfig> providers;
     private FirebaseAuth firebaseAuth;
     private FirebaseAuth.AuthStateListener listener;

     @BindView(R.id.progress_bar)
     ProgressBar progress_Bar;

     FirebaseDatabase database ;
     DatabaseReference driverInfoRef;

     @Override
     protected void onStart() {
         super.onStart();
         firebaseAuth.addAuthStateListener(listener);


     }

     @Override
     protected void onStop() {
         if (firebaseAuth != null && listener!= null)
             firebaseAuth.removeAuthStateListener(listener);
         super.onStop();
     }

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash_screen);
        init();
    }

    private void init(){

         ButterKnife.bind(this);

       database = FirebaseDatabase.getInstance();
       driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE);

        providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());
        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth -> {
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if (user!= null) {
                checkUserfromFirebse();
            }
            else
                showLoginLayout();
        };
    }

    public void checkUserfromFirebse(){
        driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists())
                        {

                       //     Toast.makeText(splashscreenActivity.this,"user already register",Toast.LENGTH_SHORT).show();

                              DriverInfoModel driverInfoModel = snapshot.getValue(DriverInfoModel.class);
                              goToHomeActivity(driverInfoModel);

                        }else {
                            showRegisterLayout();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull  DatabaseError error) {
                        Toast.makeText(splashscreenActivity.this,""+error.getMessage(),Toast.LENGTH_SHORT).show();

                    }
                });

    }

     private void goToHomeActivity(DriverInfoModel driverInfoModel ) {
         Common.currentUser = driverInfoModel;  //INIT value
         startActivity(new Intent(splashscreenActivity.this,DriverHomeActivity.class));
         finish();

     }

     private void showRegisterLayout() {

         AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.DialogTheme);
         View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null);


         EditText first_name = (EditText) itemView.findViewById(R.id.first_name);
         EditText last_name = (EditText) itemView.findViewById(R.id.last_name);
         EditText phone = (EditText) itemView.findViewById(R.id.phone);

         Button btn_continue = (Button)itemView.findViewById(R.id.btnRegister);

         // set data

         if (FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()!= null &&
                 TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))
         phone.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

         //set VIew

         builder.setView(itemView);
         AlertDialog dialog  = builder.create();
         dialog.show();

         btn_continue.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 if (TextUtils.isEmpty(first_name.getText().toString())) {
                     Toast.makeText(splashscreenActivity.this, "Please enter first name", Toast.LENGTH_SHORT).show();
                     return;
                 } else if (TextUtils.isEmpty(last_name.getText().toString())) {
                     Toast.makeText(splashscreenActivity.this, "Please enter last name", Toast.LENGTH_SHORT).show();
                     return;
                 } else if (TextUtils.isEmpty(phone.getText().toString())) {
                     Toast.makeText(splashscreenActivity.this, "Please enter phoneNumber", Toast.LENGTH_SHORT).show();

                     return;
                 } else {
                     DriverInfoModel model = new DriverInfoModel();
                     model.setFirstName(first_name.getText().toString());
                     model.setLastName(last_name.getText().toString());
                     model.setPhoneNum(phone.getText().toString());
                     model.setRating(0.0);

                     driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                             .setValue(model)
                             .addOnFailureListener(e ->
                                     {
                                         dialog.dismiss();
                                         Toast.makeText(splashscreenActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                     }
                             )
                             .addOnSuccessListener(unused -> {
                                 Toast.makeText(splashscreenActivity.this, "Register Sucessfully", Toast.LENGTH_SHORT).show();

                                 dialog.dismiss();
                                 splashscreenActivity.this.goToHomeActivity(model);
                             });
                 }
             }
         });

     }

     private void showLoginLayout() {
         AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                 .Builder(R.layout.layout_sign_in)
                 .setPhoneButtonId(R.id.btn_phone_sign_in)
                 .setGoogleButtonId(R.id.btn_google_sign_in)
                 .build();

         startActivityForResult(AuthUI.getInstance()
         .createSignInIntentBuilder()
                 .setAuthMethodPickerLayout(authMethodPickerLayout)
                 .setIsSmartLockEnabled(false)
                 .setTheme(R.style.LoginTheme)
                 .setAvailableProviders(providers)
                 .build(),LOGIN_REQUEST_CODE);
     }

     private void delaySplashSceen(){

         progress_Bar.setVisibility(View.VISIBLE);
        Completable.timer(5, TimeUnit.SECONDS,
                AndroidSchedulers.mainThread())
                .subscribe(new Action()  {
                               @Override
                               public void run() throws Throwable {
                                   firebaseAuth.addAuthStateListener(listener);
                               }
                           }
                        );
    }

     @Override
     protected void onActivityResult(int requestCode, int resultCode, @Nullable  Intent data) {
         super.onActivityResult(requestCode, resultCode, data);
         if(requestCode == LOGIN_REQUEST_CODE){
             IdpResponse response = IdpResponse.fromResultIntent(data);
             if (resultCode == RESULT_OK)
             {
                 FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
             }
             else
             {
                 Toast.makeText(this,"[ERROR]:"+response.getError().getMessage(),Toast.LENGTH_SHORT).show();
             }
         }
     }
 }