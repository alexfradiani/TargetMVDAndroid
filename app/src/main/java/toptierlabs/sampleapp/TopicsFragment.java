package toptierlabs.sampleapp;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Map;


/**
 * Fragment to handle the create new target form
 */
public class TopicsFragment extends ListFragment {

    private static final String MTAG = "TopicsFragment";

    public ArrayList<Topic> topics;

    /**
     * model for the topic items that will populate the listview
     */
    private class Topic {
        String name;

        Topic(String name) {
            this.name = name;
        }
    }

    /**
     * Custom array adapter for the listview
     */
    private class TopicAdapter extends ArrayAdapter<Topic> {
        private TopicAdapter(Context context, ArrayList<Topic> topics) {
            super(context, 0, topics);
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get item for that position
            Topic topic = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.view_topicitem, parent, false);
            }

            if (topic != null) {
                // set the name and the image
                TextView txt = (TextView) convertView.findViewById(R.id.topic_name);
                txt.setText(topic.name);

                ImageView img = (ImageView) convertView.findViewById(R.id.topic_image);
                // TODO: make images dynamic from server
                img.setImageResource(getImage(topic.name.toLowerCase()));
            }

            return convertView;
        }

        int getImage(String topic_name) {
            return getResources().getIdentifier("topic_" + topic_name, "drawable",
                    getActivity().getPackageName());
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_topics, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // start the load of topics asynch
        loadTopics();

        // list view set content
        Log.v(MTAG, "Creating topics for ListView");
        topics = new ArrayList<>();
        TopicAdapter adapter = new TopicAdapter(getContext(), topics);

        setListAdapter(adapter);
    }

    /**
     * Request to server list of topics
     */
    private void loadTopics() {
        String url = BuildConfig.SERVER + "/api/v1/topics";
        JsonArrayRequest jsArrRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray arr) {
                        Log.v(MTAG, "Response: " + arr.toString());

                        // create the topics
                        renderTopicsFromJson(arr);
                        ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();

                        // TODO: remove progress bar
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v(MTAG, "Request to server didn't work");
                        // TODO: remove progress bar

                        // signup failed. Unauthorized
                        if (error.networkResponse != null &&
                                error.networkResponse.statusCode == 401)
                            Log.v(MTAG, "Unauthorized error getting topics");

                        // TODO: Display a Toast msg for unhandled communication error
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return MainActivity.jsonHeaders(true);
            }
        };

        RequestQueue queue = Volley.newRequestQueue(getActivity());
        queue.add(jsArrRequest);
    }

    /**
     * Create the Topic objects from json
     */
    public void renderTopicsFromJson(JSONArray arr) {
        for (int i = 0; i < arr.length(); i++) {
            try {
                String name = arr.getJSONObject(i).optString("name");
                Topic topic = new Topic(name);

                topics.add(topic);
            }
            catch(JSONException ex) {
                Log.v(MTAG, ex.toString());
            }
        }
    }

}
