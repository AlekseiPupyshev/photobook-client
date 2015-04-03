package com.freecoders.photobook;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.freecoders.photobook.common.Constants;
import com.freecoders.photobook.common.Photobook;
import com.freecoders.photobook.db.ImageEntry;
import com.freecoders.photobook.gson.CommentEntryJson;
import com.freecoders.photobook.gson.ImageJson;
import com.freecoders.photobook.gson.UserProfile;
import com.freecoders.photobook.network.ServerInterface;
import com.freecoders.photobook.network.VolleySingleton;
import com.freecoders.photobook.utils.ImageUtils;
import com.freecoders.photobook.utils.MemoryLruCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class ImageDetailsActivity extends ActionBarActivity {
    private static String LOG_TAG = "ImageDetailsActivity";

    CircleImageView mAvatarImageView;
    TextView mImageTitleTextView;
    TextView mAuthorNameTextView;
    ImageView mImageView;
    TextView mLikeCountTextView;
    ImageView mLikeImageView;
    ImageView mDownloadImageView;
    LinearLayout mHeaderLayout;

    UserProfile mAuthor;
    ImageJson mImage;
    ImageEntry mGalleryImage;

    ListView mCommentsList;
    CommentListAdapter mCommentListAdapter;

    LinearLayout mButtonLike;
    TextView mButtonLikeTextView;
    LinearLayout mButtonComment;
    LinearLayout mButtonDownload;

    Boolean boolImageLiked = false;

    ImageLoader mAvatarLoader;
    ImageLoader mImageLoader;

    private String strImageID = "";
    private String likes[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        View view = View.inflate(this, R.layout.activity_image_details_header, null);

        mAvatarImageView = (CircleImageView) view.findViewById(R.id.imgAvatarDetails);
        mImageTitleTextView = (TextView) view.findViewById(R.id.textImageTitleDetails);
        mAuthorNameTextView = (TextView) view.findViewById(R.id.textImageAuthorDetails);
        mImageView = (ImageView) view.findViewById(R.id.imgViewDetails);
        mLikeCountTextView = (TextView) view.findViewById(R.id.textLikeCountDetails);
        mLikeImageView = (ImageView) view.findViewById(R.id.imgViewLike);
        //mDownloadImageView = (ImageView) view.findViewById(R.id.imgViewDownload);
        mHeaderLayout = (LinearLayout) view.findViewById(R.id.detailsHeaderLayout);
        mCommentsList = (ListView) findViewById(R.id.commentsListDetails);
        mButtonLike = (LinearLayout) findViewById(R.id.buttonLike);
        mButtonLikeTextView = (TextView) findViewById(R.id.textViewButtonLike);
        mButtonComment = (LinearLayout) findViewById(R.id.buttonComment);
        mButtonDownload = (LinearLayout) findViewById(R.id.buttonDownload);



        if (Photobook.getAvatarDiskLruCache() != null) {
            mAvatarLoader = new ImageLoader(VolleySingleton.getInstance(
                    Photobook.getMainActivity()).getRequestQueue(),
                    Photobook.getAvatarDiskLruCache());
            mImageLoader = new ImageLoader(VolleySingleton.getInstance(
                    Photobook.getMainActivity()).getRequestQueue(),
                    Photobook.getImageDiskLruCache());
        } else {
            ImageLoader.ImageCache memoryCache = new MemoryLruCache();
            mAvatarLoader = new ImageLoader(VolleySingleton.getInstance(
                    Photobook.getMainActivity()).getRequestQueue(),
                    memoryCache);
            mImageLoader = new ImageLoader(VolleySingleton.getInstance(
                    Photobook.getMainActivity()).getRequestQueue(),
                    memoryCache);
        }

        mCommentListAdapter = new CommentListAdapter(this,
                R.layout.item_comment, new ArrayList<CommentEntryJson>(),
                mAvatarLoader);
        mCommentsList.setAdapter(mCommentListAdapter);
        mCommentsList.addHeaderView(view);

        mCommentsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View v,
                                           int index, long arg3) {


                final int adapterItemPos = index-1;

                final PopupMenu mPopupComments = new PopupMenu(getApplicationContext(), mCommentsList.getChildAt(index));
                mPopupComments.getMenuInflater().inflate(R.menu.popup_menu_comments, mPopupComments.getMenu());

                mPopupComments.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        mPopupComments.dismiss();

                        if(Photobook.getPreferences().intPublicID.toString().equals(mCommentListAdapter.mCommentList.get(adapterItemPos).author_id.toString()))
                            ServerInterface.deleteCommentRequest(Photobook.getImageDetailsActivity(),
                                    String.valueOf(mCommentListAdapter.mCommentList.get(adapterItemPos).id),
                                    Photobook.getPreferences().strUserID,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            processDeleteComment(adapterItemPos);
                                        }
                                    }, null);

                        //Toast.makeText(getApplicationContext(),"Comment was deleted successfully",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });


                if(Photobook.getPreferences().intPublicID.toString().equals(mCommentListAdapter.mCommentList.get(adapterItemPos).author_id.toString()))
                    mPopupComments.show();
                else
                    Log.d(LOG_TAG,"long click : author ids: /"+Photobook.getPreferences().intPublicID+"/"+mCommentListAdapter.mCommentList.get(adapterItemPos).author_id+"/ ");


                Log.d(LOG_TAG,"long click : " +String.valueOf(mCommentListAdapter.mCommentList.get(adapterItemPos).id)+"  "+mCommentListAdapter.mCommentList.get(adapterItemPos).text);
                return true;
            }
        });

        Photobook.setImageDetailsActivity(this);

        Bundle b = getIntent().getExtras();
        if ((b != null) && (b.containsKey(Photobook.intentExtraImageDetailsSource)))
            populateView(true);
        else
            populateView(false);
    }

    public void populateView(Boolean boolImageFromGallery) {
        if (!boolImageFromGallery) {
            // Load image details from feed
            mAuthor = Photobook.getImageDetails().author;
            mImage = Photobook.getImageDetails().image;

            if (mImage.image_id != null)
                strImageID = mImage.image_id;

            ViewTreeObserver viewTree = mHeaderLayout.getViewTreeObserver();
            viewTree.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    int imgWidth = mHeaderLayout.getMeasuredWidth();
                    int imgHeight = (int) (mImage.ratio * 1.0 * imgWidth);
                    mImageView.getLayoutParams().height = imgHeight;
                    return true;
                }
            });

            if ((mImage != null) && (mImage.url_medium != null)
                    && (!mImage.url_medium.isEmpty())
                    && (URLUtil.isValidUrl(mImage.url_medium))) {
                mImageView.setTag(0);
                mImageLoader.get(mImage.url_medium, new ImageListener(mImageView));
            }

            if ((mAuthor != null) && (mAuthor.avatar != null) && (!mAuthor.avatar.isEmpty())
                    && (URLUtil.isValidUrl(mAuthor.avatar))) {
                mAvatarImageView.setTag(0);
                mImageLoader.get(mAuthor.avatar, new ImageListener(mAvatarImageView));
            }

            if (mImage.title != null)
                mImageTitleTextView.setText(mImage.title);

            if (mAuthor.name != null)
                mAuthorNameTextView.setText(mAuthor.name);

            if (mImage.likes != null) {
                mLikeCountTextView.setText(String.valueOf(mImage.likes.length));
                likes = mImage.likes;
            }

            mButtonLikeTextView.setText(R.string.btn_like);
            for (String id : Photobook.getImageDetails().image.likes)
                if (id.equals(Photobook.getPreferences().intPublicID.toString())) {
                    mButtonLikeTextView.setText(R.string.btn_unlike);
                    boolImageLiked = true;
                    mButtonLikeTextView.setText(R.string.btn_unlike);
                    break;
                }
        } else {
            // Load image details from gallery
            mGalleryImage = Photobook.getGalleryImageDetails();

            if (mGalleryImage.getServerId() != null)
                strImageID = mGalleryImage.getServerId();

            Bitmap b = ImageUtils.decodeSampledBitmap(mGalleryImage.getOrigUri());
            mImageView.setImageBitmap(b);

            if (mGalleryImage.getTitle() != null)
                mImageTitleTextView.setText(mGalleryImage.getTitle());

            mAuthorNameTextView.setText(getResources().getString(R.string.author_name_self));

            mLikeCountTextView.setText("0");

            mButtonLikeTextView.setText(R.string.btn_like);

            ServerInterface.getImageDetailsRequestJson(Photobook.getImageDetailsActivity(),
                mGalleryImage.getServerId(),
                new Response.Listener<HashMap<String, ImageJson>>() {
                    @Override
                    public void onResponse(HashMap<String, ImageJson> response) {
                        if (response.containsKey(mGalleryImage.getServerId())) {
                            ImageJson image = response.get(mGalleryImage.getServerId());
                            if (image.likes != null) {
                                for (String id : image.likes)
                                    if (id.equals(Photobook.getPreferences().intPublicID.
                                            toString())) {
                                        mButtonLikeTextView.setText(R.string.btn_unlike);
                                        boolImageLiked = true;
                                        break;
                                    }
                                mLikeCountTextView.setText(
                                        String.valueOf(image.likes.length));
                                likes = image.likes;
                            }
                        }
                    }
                }, null);
        }

        mButtonLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!boolImageLiked)
                    ServerInterface.likeRequest(Photobook.getImageDetailsActivity(),
                            strImageID,
                            Photobook.getPreferences().strUserID,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    processLike();
                                }
                            }, null);
                else
                    ServerInterface.unLikeRequest(Photobook.getImageDetailsActivity(),
                            strImageID,
                            Photobook.getPreferences().strUserID,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    processUnlike();
                                }
                            }, null);
            }
        });

        mButtonComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(
                        Photobook.getImageDetailsActivity());
                alert.setTitle(R.string.alert_title_comment);
                alert.setMessage(R.string.alert_message_comment);
                final EditText input = new EditText(Photobook.getImageDetailsActivity());
                alert.setView(input);
                alert.setPositiveButton(R.string.alert_send_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String strComment = input.getText().toString();
                                ServerInterface.postCommentRequest(
                                        Photobook.getImageDetailsActivity(),
                                        strImageID,
                                        Photobook.getPreferences().strUserID,
                                        strComment, 0,
                                        new Response.Listener<String>() {
                                            @Override
                                            public void onResponse(String response) {
                                                ServerInterface.getComments(
                                                        Photobook.getImageDetailsActivity(),
                                                        strImageID,
                                                        mCommentListAdapter);
                                            }
                                        }, null);
                            }
                        });
                alert.setNegativeButton(R.string.alert_cancel_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });
                alert.show();
            }
        });

        mButtonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((Photobook.getImageDetails().image.url_original != null)
                        && (URLUtil.isValidUrl(Photobook.getImageDetails().image.url_original)))
                    new DownloadImage().execute(Photobook.getImageDetails().image.url_original);
            }
        });

        ServerInterface.getComments(this, strImageID, mCommentListAdapter);

        if (Photobook.getPreferences().hsetUnreadImages.contains(strImageID)) {
            Photobook.getPreferences().hsetUnreadImages.remove(strImageID);
            Photobook.getPreferences().unreadImagesMap.put(strImageID, 0);
            Photobook.getPreferences().savePreferences();
            Photobook.getGalleryFragmentTab().refreshAdapter();
        }
    }

    public void processLike () {
        Integer intLikeCount =
                Integer.valueOf(mLikeCountTextView.getText().
                        toString() ) + 1;
        mLikeCountTextView.setText(intLikeCount.toString());
        mButtonLikeTextView.setText(R.string.btn_unlike);
        boolImageLiked = true;
        if (likes != null) {
            String[] newLikeList = new String[likes.length + 1];
            Integer i = 0;
            for(String id : likes) {
                newLikeList[i] = id;
                i++;
            }
            newLikeList[i] = Photobook.getPreferences().intPublicID.toString();
            likes = newLikeList;
            if ((Photobook.getImageDetails()!=null) &&
                    (Photobook.getImageDetails().image!=null))
                Photobook.getImageDetails().image.likes = newLikeList;
        }
    }

    public void processUnlike() {
        mButtonLikeTextView.setText(R.string.btn_like);
        Integer intLikeCount =
                Integer.valueOf(mLikeCountTextView.getText().
                        toString() ) - 1;
        intLikeCount = intLikeCount < 0 ? 0 : intLikeCount;
        mLikeCountTextView.setText(intLikeCount.toString());
        boolImageLiked = false;
        if (likes != null) {
            String[] newLikeList = new String[likes.length - 1];
            Integer i = 0;
            for(String id : likes)
                if (!id.equals(Photobook.getPreferences().intPublicID.toString())) {
                    newLikeList[i] = id;
                    i++;
                }
            likes = newLikeList;
            if ((Photobook.getImageDetails()!=null) &&
                    (Photobook.getImageDetails().image!=null))
                Photobook.getImageDetails().image.likes = newLikeList;
        }
    }

    public void processDeleteComment(int index) {
        mCommentListAdapter.mCommentList.remove(index);
        mCommentListAdapter.notifyDataSetChanged();
    }

    class DownloadImage extends AsyncTask<String,Integer,Long> {
        String strFilename = "";
        ProgressDialog mProgressDialog = new ProgressDialog(Photobook.getImageDetailsActivity());
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.setMessage(getResources().getString(R.string.dialog_downloading));
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.show();
        }
        @Override
        protected Long doInBackground(String... aurl) {
            int count;
            try {
                URL url = new URL((String) aurl[0]);
                URLConnection conexion = url.openConnection();
                conexion.connect();
                String targetFileName =
                        aurl[0].substring(aurl[0].lastIndexOf('/')+1, aurl[0].length() );
                int lenghtOfFile = conexion.getContentLength();
                String type = Environment.DIRECTORY_PICTURES;
                File path = Environment.getExternalStoragePublicDirectory(type);
                path.mkdirs();
                File file = new File(path, Constants.APP_FOLDER +"/" + targetFileName);
                if(!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                strFilename = file.toString();
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(file);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress ((int)(total*100/lenghtOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
                e.printStackTrace();
                mProgressDialog.dismiss();
            }
            return null;
        }
        protected void onProgressUpdate(Integer... progress) {
            mProgressDialog.setProgress(progress[0]);
            if(mProgressDialog.getProgress()==mProgressDialog.getMax()){
                mProgressDialog.dismiss();
                Toast.makeText(Photobook.getImageDetailsActivity() ,
                        getResources().getString(R.string.dialog_download_complete)
                        , Toast.LENGTH_SHORT).show();

                if (!strFilename.isEmpty())
                    MediaScannerConnection.scanFile(Photobook.getImageDetailsActivity(),
                        new String[]{strFilename}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                            }
                        });
            }
        }
        protected void onPostExecute(String result) {
        }
    }

    public void getLikesListView(View view){
        int width = ImageUtils.dpToPx(300);
        final int height = ImageUtils.dpToPx(50);
        final int padding = 2;
        final Context context = this;
        final HorizontalScrollView scrollView = new HorizontalScrollView(context);
        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollView.setPadding(padding, padding, padding, padding);
        scrollView.setLayoutParams(params);
        scrollView.addView(linearLayout);
        scrollView.setHorizontalScrollBarEnabled(false);
        final PopupWindow popup = new PopupWindow(context);
        popup.setContentView(scrollView);
        popup.setBackgroundDrawable(getResources().getDrawable(R.drawable.popup));
        popup.setWidth(width);
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);
        int[] location = new int[2];
        mLikeCountTextView.getLocationOnScreen(location);
        popup.showAtLocation(scrollView, Gravity.NO_GRAVITY, location[0],
                location[1] - (int) (height * 1.2));
        if (likes == null) return;
        ServerInterface.getUserProfileRequest(this, likes,
            new Response.Listener<HashMap<String, UserProfile>>() {
                @Override
                public void onResponse(HashMap<String, UserProfile> response) {
                    Iterator it = response.entrySet().iterator();
                    Log.d(LOG_TAG, "Response size " + response.size());
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        ImageView image = new ImageView(context);
                        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(height, height);
                        image.setLayoutParams(params);
                        image.setPadding(padding, padding, padding, padding);
                        image.setImageResource(R.drawable.avatar);
                        linearLayout.addView(image);
                        final UserProfile user = (UserProfile) pair.getValue();
                        final String id = (String) pair.getKey();
                        if ((user != null) && (user.avatar != null)
                                && (URLUtil.isValidUrl(user.avatar)))
                            mAvatarLoader.get(user.avatar, new ImageListener(image));
                        image.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                FragmentManager fm = getFragmentManager();
                                UserProfileFragment profileDialogFragment =
                                        new UserProfileFragment();
                                profileDialogFragment.setUserId(id);
                                profileDialogFragment.show(fm, "users_profile");
                            }
                        });
                    }
                }
            }, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_image_details, menu);
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

    private class ImageListener implements ImageLoader.ImageListener {
        ImageView imgView;

        public ImageListener(ImageView imgView) {
            this.imgView = imgView;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
        }

        @Override
        public void onResponse(ImageLoader.ImageContainer response, boolean arg1) {
            if (response.getBitmap() != null) {
                imgView.setImageResource(0);
                imgView.setImageBitmap(response.getBitmap());
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (Photobook.getPreferences().hsetUnreadImages.contains(strImageID)) {
            Photobook.getPreferences().hsetUnreadImages.remove(strImageID);
            Photobook.getPreferences().unreadImagesMap.put(strImageID, 0);
            Photobook.getPreferences().savePreferences();
            Photobook.getGalleryFragmentTab().refreshAdapter();
        }
    }
}
