package com.shopify.mobilebuy.sample2;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.shopify.buy3.GraphCall;
import com.shopify.buy3.GraphClient;
import com.shopify.buy3.GraphError;
import com.shopify.buy3.Storefront;

import java.util.ArrayList;

import okhttp3.OkHttpClient;

public class CollectionActivity extends AppCompatActivity {
    private ArrayList<String> collectionNames;
    private ArrayAdapter<String> collectionNamesAdapter;
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
        collectionNamesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, collectionNames);
        collectionsList.setAdapter(collectionNamesAdapter);

        new Thread(() -> addCollectionNames(queryCollections("", 5))).start();
    }

    private void addCollectionNames(Storefront.QueryRoot root) {
        root.getShop().getCollections().getEdges().forEach(
                e -> collectionNames.add(e.getNode().getTitle())
        );
        if (collectionNamesAdapter != null) {
            runOnUiThread(() -> collectionNamesAdapter.notifyDataSetChanged());
        }
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

    private Storefront.QueryRoot queryCollections(@Nullable final String cursor, final int numPerPage){
        Storefront.QueryRoot ret = null;
           GraphCall<Storefront.QueryRoot> c = graphClient.queryGraph(Storefront.query(
                root -> root.shop(
                        shop -> shop.collections(
                                5,
                                args -> args
                                        .after(null)
                                        .sortKey(Storefront.CollectionSortKeys.TITLE),
                                collectionConnection -> collectionConnection
                                        .edges(collectionEdge -> collectionEdge
                                                .cursor()
                                                .node(collection -> collection
                                                        .title()
                                                        .description()
                                                        .image(Storefront.ImageQuery::src)
                                                        .products(5, productConnection -> productConnection
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
            Log.d("SAMPLE", ret==null ? "NULL":"gucci");
        } catch (GraphError e) {
            Log.e("SAMPLE", e.toString());
        }
        return ret;
    }
}
