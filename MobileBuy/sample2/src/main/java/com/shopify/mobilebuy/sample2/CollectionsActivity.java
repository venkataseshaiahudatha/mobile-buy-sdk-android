package com.shopify.mobilebuy.sample2;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.exception.ApolloException;
import com.shopify.mobilebuy.sample2.CollectionsWithProducts.Data.Edge;
import com.shopify.mobilebuy.sample2.type.CollectionSortKeys;
import com.shopify.mobilebuy.sample2.type.CustomType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class CollectionsActivity extends AppCompatActivity {
    private ArrayList<CollectionsWithProducts.Data.Collection> collectionNames;
    private CollectionsAdapter collectionNamesAdapter;
    private ListView collectionsList;

    private static final String SHOP_PROPERTIES_INSTRUCTION =
            "\n\tAdd your shop credentials to a shop.properties file in the main app folder (e.g. 'app/shop.properties')."
                    + "Include these keys:\n" + "\t\tSHOP_DOMAIN=<myshop>.myshopify.com\n"
                    + "\t\tAPI_KEY=0123456789abcdefghijklmnopqrstuvw\n";

    private static ApolloClient apolloClient;

    public static ApolloClient apolloClient() {
        return apolloClient;
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
                CollectionsWithProducts.Data.Collection item = (CollectionsWithProducts.Data.Collection)parent.getItemAtPosition(position);
                Intent myIntent = new Intent(CollectionsActivity.this, ProductsActivity.class);
                myIntent.putExtra("Title", item.title());
                myIntent.putExtra("Id", item.id());
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

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder().method(original.method(), original.body());
                    builder.header("User-Agent", "Android Apollo Client");
                    builder.header("X-Shopify-Storefront-Access-Token", shopifyApiKey);
                    return chain.proceed(builder.build());
                })
                .build();

        apolloClient = ApolloClient.builder()
                .okHttpClient(httpClient)
                .serverUrl(HttpUrl.parse("https://" + shopUrl + "/api/graphql"))
                .withCustomTypeAdapter(CustomType.MONEY, new CustomTypeAdapter<BigDecimal>() {
                    @Override public BigDecimal decode(final String value) {
                        return new BigDecimal(value);
                    }

                    @Override public String encode(final BigDecimal value) {
                        return value.toString();
                    }
                })
                .build();
    }

    private void addCollectionNames(CollectionsWithProducts.Data data) {
        List<Edge> edges = data.shop().collectionConnection().edges();

        String cursor = "";
        for (Edge e : edges) {
            collectionNames.add(e.collection());
            cursor = e.cursor();
        }

        runOnUiThread(() -> collectionNamesAdapter.notifyDataSetChanged());
        if (edges.size() == 5) {
            addCollectionNames(queryCollections(cursor, 5));
        }
    }

    private CollectionsWithProducts.Data queryCollections(@Nullable final String cursor, final int numPerPage){
        CollectionsWithProducts.Data ret = null;

        CollectionsWithProducts query = CollectionsWithProducts.builder()
                .perPage(numPerPage)
                .nextPageCursor(TextUtils.isEmpty(cursor) ? null : cursor)
                .collectionSortKey(CollectionSortKeys.TITLE)
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
