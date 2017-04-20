package com.shopify.mobilebuy.sample2;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.exception.ApolloException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sammaier on 4/21/17.
 */

public class ProductsActivity extends AppCompatActivity {
    private ArrayList<String> productNames;
    private ArrayAdapter<String> productNamesAdapter;
    private ListView productsList;
    private String collectionId;
    private final ApolloClient apolloClient = CollectionsActivity.apolloClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        productsList = (ListView) findViewById(R.id.productsList);
        productNames = new ArrayList<>();
        productNamesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, productNames);
        productsList.setAdapter(productNamesAdapter);

        Intent intent = getIntent();
        collectionId = intent.getStringExtra("Id");
        getSupportActionBar().setTitle(intent.getStringExtra("Title"));
        new Thread(() -> addProductNames(queryProducts(collectionId, "", 5))).start();
    }

    private void addProductNames(CollectionProducts.Data data) {
        List<CollectionProducts.Data.ProductEdge> edges = data.collection().asCollection().productConnection().productEdges();

        String cursor = "";
        for (CollectionProducts.Data.ProductEdge e : edges) {
            productNames.add(e.product().title());
            cursor = e.cursor();
        }

        runOnUiThread(() -> productNamesAdapter.notifyDataSetChanged());
        if (edges.size() == 5) {
            addProductNames(queryProducts(collectionId, cursor, 5));
        }
    }

    private CollectionProducts.Data queryProducts(String collectionId, @Nullable final String cursor, final int numPerPage) {
        CollectionProducts.Data ret = null;

        CollectionProducts query = CollectionProducts.builder()
                .perPage(numPerPage)
                .nextPageCursor(TextUtils.isEmpty(cursor) ? null : cursor)
                .collectionId(collectionId)
                .build();

        try {
            ret = apolloClient.newCall(query).execute().data();
        } catch (ApolloException e) {
            e.printStackTrace();
            Log.e("SAMPLE", e.toString());
        }
        return ret;
    }
}
