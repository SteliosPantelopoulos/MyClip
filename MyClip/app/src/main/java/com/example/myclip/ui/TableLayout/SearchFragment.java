package com.example.myclip.ui.TableLayout;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.example.myclip.R;
import com.example.myclip.model.Consumer;
import com.example.myclip.model.User;
import com.example.myclip.ui.TableLayout.Topics.TopicFragment;

import java.util.ArrayList;

public class SearchFragment extends Fragment {

    private String querySearched;
    private SearchView searchView;

    private Consumer consumer = new User().getConsumer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);

        searchView = rootView.findViewById(R.id.fragment_search_input);


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //called when enter is pressed
                querySearched = query;

                FragmentManager fragmentManager = getChildFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                Fragment topicFragment = new TopicFragment();
                fragmentTransaction.replace(R.id.fragment_search_fragment_view, topicFragment);
                fragmentTransaction.commit();

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return rootView;
    }


    public ArrayList<String> getTopics(){
        /**
         * called from search fragment
         * return all topics that start with "query"
         */
        String query = querySearched;
        ArrayList<String> topics = new ArrayList<>();
        if(query!=null){
            if(query.startsWith("#")){
                for (ArrayList<String> hashtagsOfBroker : consumer.getBrokersHashtagsResponsible().values()) {
                    for(String hashtag: hashtagsOfBroker){
                        if(hashtag.startsWith(query)){
                            topics.add(hashtag);
                        }
                    }
                }

            }else{
                for (ArrayList<String> channelsOfBroker : consumer.getBrokersChannelsResponsible().values()) {
                    for(String channel: channelsOfBroker){
                        if(!channel.equals(new User().getChannel().getChannelName()) && channel.startsWith(query)){
                            topics.add(channel);
                        }
                    }
                }
            }
        }
        return topics;
    }
}