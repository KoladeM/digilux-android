package com.tunjid.fingergestures.fragments;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.PackageAdapter;
import com.tunjid.fingergestures.baseclasses.MainActivityFragment;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import static android.support.v7.widget.DividerItemDecoration.VERTICAL;
import static com.tunjid.fingergestures.adapters.AppAdapter.ROTATION_LOCK;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

public class PackageFragment extends MainActivityFragment implements PackageAdapter.PackageClickListener {


    private View progressBar;
    private RecyclerView recyclerView;
    private static final List<String> packageNames = new ArrayList<>();

    public static PackageFragment newInstance() {
        PackageFragment fragment = new PackageFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public String getStableTag() {
        return getClass().getSimpleName();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_packages, container, false);
        Context context = inflater.getContext();

        DividerItemDecoration itemDecoration = new DividerItemDecoration(context, VERTICAL);
        Drawable decoration = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_dark);

        if (decoration != null) itemDecoration.setDrawable(decoration);

        progressBar = root.findViewById(R.id.progress_bar);
        recyclerView = root.findViewById(R.id.options_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new PackageAdapter(false, packageNames, this));
        recyclerView.addItemDecoration(itemDecoration);

        populateList(context);

        return root;
    }


    @Override
    public void onPackageClicked(String packageName) {
        boolean added = RotationGestureConsumer.getInstance().addRotationApp(packageName);

        if (!added) {
            Context context = recyclerView.getContext();
            new AlertDialog.Builder(context)
                    .setTitle(R.string.go_premium_title)
                    .setMessage(context.getString(R.string.go_premium_body, context.getString(R.string.auto_rotate_premium_description)))
                    .setPositiveButton(R.string.continue_text, (dialog, which) -> purchase(PurchasesManager.PREMIUM_SKU))
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        toggleBottomSheet(false);

        AppFragment fragment = getCurrentAppFragment();
        if (fragment == null) return;

        fragment.refresh(ROTATION_LOCK);
    }

    @Override
    public void onDestroyView() {
        recyclerView = null;
        super.onDestroyView();
    }

    private void populateList(Context context) {
        Single.fromCallable(() -> context.getPackageManager().getInstalledApplications(0).stream()
                .filter(applicationInfo -> (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 || (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
                .map(packageInfo -> packageInfo.packageName)
                .sorted()
                .collect(Collectors.toList()))
                .subscribeOn(Schedulers.io())
                .observeOn(mainThread())
                .subscribe(list -> {
                    ViewGroup root = (ViewGroup) getView();
                    if (root == null) return;

                    packageNames.clear();
                    packageNames.addAll(list);
                    progressBar.setVisibility(View.GONE);
                    recyclerView.getAdapter().notifyDataSetChanged();
                    TransitionManager.beginDelayedTransition(root, new AutoTransition());
                }, Throwable::printStackTrace);
    }
}
