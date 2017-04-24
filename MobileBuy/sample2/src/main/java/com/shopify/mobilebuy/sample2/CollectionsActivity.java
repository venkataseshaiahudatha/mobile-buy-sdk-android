package com.shopify.mobilebuy.sample2;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.shopify.buy3.GraphCall;
import com.shopify.buy3.GraphClient;
import com.shopify.buy3.GraphError;
import com.shopify.buy3.Storefront;
import com.shopify.graphql.support.ID;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

public class CollectionsActivity extends AppCompatActivity {
    private ArrayList<Storefront.Collection> collectionNames;
    private CollectionsAdapter collectionNamesAdapter;
    private ListView collectionsList;

    private static final String SHOP_PROPERTIES_INSTRUCTION =
            "\n\tAdd your shop credentials to a shop.properties file in the main app folder (e.g. 'app/shop.properties')."
                    + "Include these keys:\n" + "\t\tSHOP_DOMAIN=<myshop>.myshopify.com\n"
                    + "\t\tAPI_KEY=0123456789abcdefghijklmnopqrstuvw\n";

    private static GraphClient graphClient;

    public static GraphClient graphClient() {
        return graphClient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeGraphClient();
        setContentView(R.layout.activity_collection);

        collectionsList = (ListView) findViewById(R.id.collectionsList);
        collectionNames = new ArrayList<>();
        collectionNamesAdapter = new CollectionsAdapter(this,
                android.R.layout.simple_list_item_1, collectionNames);
        collectionsList.setAdapter(collectionNamesAdapter);

        collectionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Storefront.Collection item = (Storefront.Collection)parent.getItemAtPosition(position);
                Intent myIntent = new Intent(CollectionsActivity.this, ProductsActivity.class);
                myIntent.putExtra("Title", item.getTitle());
                myIntent.putExtra("Id", item.getId().toString());
                CollectionsActivity.this.startActivity(myIntent);
            }
        });

        new Thread(() -> addCollectionNames(queryCollections("", 5))).start();
    }

    private void initializeGraphClient() {
        String shopUrl = BuildConfig.SHOP_DOMAIN;
        if (TextUtils.isEmpty(shopUrl)) {
            throw new IllegalArgumentException(SHOP_PROPERTIES_INSTRUCTION + "You must add 'SHOP_DOMAIN' entry in "
                    + "app/shop.properties, in the form '<myshop>.myshopify.com'");
        }

        String shopifyApiKey = BuildConfig.API_KEY;
        if (TextUtils.isEmpty(shopifyApiKey)) {
            throw new IllegalArgumentException(SHOP_PROPERTIES_INSTRUCTION + "You must populate the 'API_KEY' entry in "
                    + "app/shop.properties");
        }

        OkHttpClient httpClient = new OkHttpClient.Builder().build();

        graphClient = GraphClient.builder(this)
                .shopDomain(BuildConfig.SHOP_DOMAIN)
                .accessToken(BuildConfig.API_KEY)
                .httpClient(httpClient)
                .build();
    }

    private void addCollectionNames(Storefront.QueryRoot root) {
        List<Storefront.CollectionEdge> edges = root.getShop().getCollections().getEdges();

        String cursor = "";
        for (Storefront.CollectionEdge e : edges) {
            collectionNames.add(e.getNode());
            cursor = e.getCursor();
        }

        runOnUiThread(() -> collectionNamesAdapter.notifyDataSetChanged());
        if (edges.size() == 5) {
            addCollectionNames(queryCollections(cursor, 5));
        }
    }

    private Storefront.QueryRoot queryCollections(@Nullable final String cursor, final int numPerPage){
        Storefront.QueryRoot ret = null;
           GraphCall<Storefront.QueryRoot> c = graphClient.queryGraph(Storefront.query(
                root -> root.shop(
                        shop -> shop.collections(
                                numPerPage,
                                args -> args
                                        .after(TextUtils.isEmpty(cursor) ? null : cursor)
                                        .sortKey(Storefront.CollectionSortKeys.TITLE),
                                collectionConnection -> collectionConnection
                                        .edges(collectionEdge -> collectionEdge
                                                .cursor()
                                                .node(collection -> collection
                                                        .title()
                                                        .description()
                                                        .image(Storefront.ImageQuery::src)
                                                        .products(numPerPage, productConnection -> productConnection
                                                                .edges(productEdge -> productEdge
                                                                        .cursor()
                                                                        .node(product -> product
                                                                                .title()
                                                                                .images(1, imageConnection -> imageConnection
                                                                                        .edges(imageEdge -> imageEdge
                                                                                                .node(Storefront.ImageQuery::src)))
                                                                                .variants(250, variantConnection -> variantConnection
                                                                                        .edges(variantEdge -> variantEdge
                                                                                                .node(Storefront.ProductVariantQuery::price)))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                        )
                )
          ));
        try {
            ret = c.execute().data();
        } catch (GraphError e) {
            Log.e("SAMPLE", e.toString());
        }
        return ret;
    }
}
