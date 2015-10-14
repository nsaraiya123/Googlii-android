package com.googlii.adapters;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.googlii.R;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by niravsaraiya on 10/13/15.
 */
public class ScheduleRecyclerAdapter extends RecyclerView.Adapter<ScheduleRecyclerAdapter.ListViewHolder> {
    Context context;
    OnItemClickListener clickListener;
    List<ParseObject>schedules;
    List<Integer>selectedMatches;
    List<ParseObject>savedMatches;

    public ScheduleRecyclerAdapter(Context context) {
        this.context = context;
        getSchedulesFromParse();
        selectedMatches = new ArrayList<Integer>();
        clickListener = new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
            }
        };
    }

    private void getSchedulesFromParse()
    {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("UserMatch");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.setLimit(100);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> savedMatchList, ParseException e) {
                if (e == null) {
                    savedMatches = savedMatchList;
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Schedule");
                    query.whereGreaterThan("date", new Date());
                    query.setLimit(100);
                    query.findInBackground(new FindCallback<ParseObject>() {
                        public void done(List<ParseObject> scoreList, ParseException e) {
                            if (e == null) {
                                schedules = scoreList;
                                notifyDataSetChanged();
                                Log.d("score", "Retrieved " + scoreList.size() + " scores");
                            } else {
                                Log.d("score", "Error: " + e.getMessage());
                            }
                        }
                    });
                    Log.d("savedMatchList", "Retrieved " + savedMatchList.size() + " saved matches");
                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void saveSelectedMatches()
    {
        if (selectedMatches.isEmpty())
        {
            Toast.makeText(context, R.string.no_matches_selected, Toast.LENGTH_LONG);
            return;
        }
        List<ParseObject>userMatches = new ArrayList<ParseObject>();
        ParseUser currentUser = ParseUser.getCurrentUser();

        for (Integer i : selectedMatches)
        {
            ParseObject schedule = schedules.get(i);
            ParseObject userMatch = new ParseObject("UserMatch");
            userMatch.put("user", currentUser);
            userMatch.put("litzID", schedule.get("litzID"));
            userMatches.add(userMatch);
        }
        ParseObject.saveAllInBackground(userMatches);
    }

    @Override
    public ListViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        CardView view = (CardView)LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.schedulelist_item, viewGroup, false);
        ListViewHolder listViewHolder = new ListViewHolder(view);

        return listViewHolder;
    }

    @Override
    public void onBindViewHolder(final ListViewHolder listViewHolder, final int i) {
        String title = schedules.get(i).getString("shortName");
        Date date = schedules.get(i).getDate("date");
        SimpleDateFormat format = new SimpleDateFormat("dd MMM");
        String strDate = format.format(date);
        listViewHolder.title.setText(title + " " + strDate);
        listViewHolder.subTitle.setText(schedules.get(i).getString("relatedName"));

        String thisScheduleMatchId = schedules.get(i).getString("litzID");
        listViewHolder.checkbox.setVisibility(View.VISIBLE);
        listViewHolder.title.setTextColor(context.getResources().getColor(R.color.text_primary));
        listViewHolder.subTitle.setTextColor(context.getResources().getColor(R.color.text_secondary));
        for (ParseObject savedMatch : savedMatches)
        {
            if (thisScheduleMatchId.equalsIgnoreCase(savedMatch.getString("litzID")))
            {
                listViewHolder.checkbox.setVisibility(View.INVISIBLE);
                listViewHolder.title.setTextColor(context.getResources().getColor(R.color.accent_500));
                listViewHolder.subTitle.setTextColor(context.getResources().getColor(R.color.accent_700));
                return;
            }
        }

        listViewHolder.checkbox.setChecked(false);
        for (Integer j : selectedMatches)
            if (j.intValue() == i) {
                listViewHolder.checkbox.setChecked(true);
                break;
            }

        listViewHolder.checkbox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_UP)
                    return false;
                if (listViewHolder.checkbox.isChecked() == false){
                    listViewHolder.checkbox.setChecked(true);
                    selectedMatches.add(new Integer(i));
                }
                else{
                    listViewHolder.checkbox.setChecked(false);
                    int k = 0;
                    for (Integer j : selectedMatches) {
                        if (j.intValue() == i) {
                            selectedMatches.remove(k);
                            break;
                        }
                        k++;
                    }
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        if (schedules == null)
            return 0;
        else
            return schedules.size();
    }

    class ListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CardView cardItemLayout;
        TextView title;
        TextView subTitle;
        CheckBox checkbox;

        public ListViewHolder(View itemView) {
            super(itemView);

            cardItemLayout = (CardView) itemView.findViewById(R.id.cardlist_item);
            title = (TextView) itemView.findViewById(R.id.listitem_name);
            subTitle = (TextView) itemView.findViewById(R.id.listitem_subname);
            checkbox = (CheckBox) itemView.findViewById(R.id.checkBox);

            itemView.setOnClickListener(this);
            itemView.setTag(this);
        }

        @Override
        public void onClick(View v)
        {
            clickListener.onItemClick(v, getAdapterPosition());
        }
    }

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
    }
//
//    public void SetOnItemClickListener(final OnItemClickListener itemClickListener) {
//        this.clickListener = itemClickListener;
//    }
}
