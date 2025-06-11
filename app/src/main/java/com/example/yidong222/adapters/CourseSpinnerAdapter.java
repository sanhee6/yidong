package com.example.yidong222.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.yidong222.R;

import java.util.List;

/**
 * 自定义课程下拉列表适配器，确保下拉列表可以正常选择
 */
public class CourseSpinnerAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final List<String> courseNames;
    private final LayoutInflater inflater;

    public CourseSpinnerAdapter(Context context, List<String> courseNames) {
        super(context, R.layout.item_spinner, courseNames);
        this.context = context;
        this.courseNames = courseNames;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        }

        TextView textView = (TextView) view;
        if (position < courseNames.size()) {
            textView.setText(courseNames.get(position));
        }

        return view;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }

        TextView textView = (TextView) view;
        if (position < courseNames.size()) {
            textView.setText(courseNames.get(position));
        }

        return view;
    }
}