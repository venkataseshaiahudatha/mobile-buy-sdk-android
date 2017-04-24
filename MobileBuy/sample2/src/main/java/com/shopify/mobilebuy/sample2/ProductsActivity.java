package com.shopify.mobilebuy.sample2;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.shopify.buy3.GraphCall;
import com.shopify.buy3.GraphClient;
import com.shopify.buy3.GraphError;
import com.shopify.buy3.Storefront;
import com.shopify.graphql.support.ID;

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
    private final GraphClient graphClient = CollectionsActivity.graphClient();

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

    private void addProductNames(Storefront.QueryRoot root) {
        List<Storefront.ProductEdge> edges = ((Storefront.Collection)root.getNode()).getProducts().getEdges();

        String cursor = "";
        for (Storefront.ProductEdge e : edges) {
            productNames.add(e.getNode().getTitle());
            cursor = e.getCursor();
        }

        runOnUiThread(() -> productNamesAdapter.notifyDataSetChanged());
        if (edges.size() == 5) {
            addProductNames(queryProducts(collectionId, cursor, 5));
        }
    }

    private Storefront.QueryRoot queryProducts(String collectionId, @Nullable final String cursor, final int numPerPage) {
        Storefront.QueryRoot ret = null;
        GraphCall<Storefront.QueryRoot> call = graphClient.queryGraph(Storefront.query(
                root -> root
                        .node(new ID(collectionId), node -> node
                                .onCollection(collectionConnection -> collectionConnection
                                        .products(
                                                numPerPage,
                                                args -> args.after(TextUtils.isEmpty(cursor) ? null : cursor),
                                                productConnection -> productConnection
                                                        .edges(productEdge -> productEdge
                                                                .cursor()
                                                                .node(product -> product
                                                                        .title()
                                                                        .images(1, imageConnection -> imageConnection
                                                                                .edges(imageEdge -> imageEdge
                                                                                        .node(Storefront.ImageQuery::src)
                                                                                )
                                                                        )
                                                                        .variants(250, variantConnection -> variantConnection
                                                                                .edges(variantEdge -> variantEdge
                                                                                        .node(Storefront.ProductVariantQuery::price)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                        )
                                )
                        )
        ));
        try {
            ret = call.execute().data();
        } catch (GraphError e) {
            Log.e("SAMPLE", e.toString());
        }
        return ret;
    }
}
