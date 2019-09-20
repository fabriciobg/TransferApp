package com.example.transferapp;

import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DirectionFinderListener {

    private GoogleMap mMap;
    private Button btnTracaRota, btnCalcular, btnTaxas;
    private EditText edtOrigem, edtDestino;
    private TextView txtDistancia, txtDuracao, txtPrecoTotal;
    private List<Marker> originMarker = new ArrayList<>();
    private List<Marker> destinationMarker = new ArrayList<>();
    private List<Polyline> polylinePaths;// = new ArrayList<>();
    private ProgressDialog progressDialog;
    private Double taxaPorQuilometro = 0.8;
    private Double taxaPorMinuto = 0.2;
    private int durationValue = 0, distanceValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Instacia dos objetos
        btnTracaRota = (Button) findViewById(R.id.btnTracaRota);
        edtOrigem = (EditText) findViewById(R.id.edtOrigem);
        edtDestino = (EditText) findViewById(R.id.edtDestino);
        txtDistancia = (TextView) findViewById(R.id.txtDistancia);
        txtDuracao = (TextView) findViewById(R.id.txtDuracao);
        txtPrecoTotal = findViewById(R.id.txtPrecoTotal);
        btnCalcular = findViewById(R.id.btnCalcular);
        btnTaxas = (Button) findViewById(R.id.btnTaxas);


        //evento Click do botao
        btnTracaRota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // chamar a funcao que traca a rota
                TracaRota();
            }

        });

        btnCalcular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // chamar a funçao que calcula o preço da viagem
                CalcularValorDaViagem();
            }
        });


        btnTaxas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.taxes_dialog, null);

                // Instanciando os componentes
                final EditText edtTaxaPorQuilometro = (EditText) mView.findViewById(R.id.edtTaxaPorQuilometro);
                final EditText edtTaxaPorMinuto = (EditText) mView.findViewById(R.id.edtTaxaPorMinuto);
                Button btnConfirmar = (Button) mView.findViewById(R.id.btnConfirmar);

                // Adicionando os valores das taxas nos edit texts
                DecimalFormat df = new DecimalFormat("#0.00");
                edtTaxaPorQuilometro.setText("R$" + df.format(taxaPorQuilometro).replace('.', ','));
                edtTaxaPorMinuto.setText("R$" + df.format(taxaPorMinuto).replace('.',',' ));


                mBuilder.setView(mView);
                final AlertDialog dialog = mBuilder.create();

                // Adicionando uma máscara para garantir o formato da moeda
                edtTaxaPorQuilometro.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    private String current = "";
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if(!s.toString().equals(current)) {
                            Locale myLocale = new Locale("pt", "BR");
                            //Nesse bloco ele monta a maskara para money
                            edtTaxaPorQuilometro.removeTextChangedListener(this);
                            String cleanString = s.toString().replaceAll("[R$,.]", "");
                            double parsed = Double.parseDouble(cleanString);
                            String formatted = NumberFormat.getCurrencyInstance(myLocale).format((parsed / 100));
                            current = formatted;
                            edtTaxaPorQuilometro.setText(formatted);
                            edtTaxaPorQuilometro.setSelection(formatted.length());

                            edtTaxaPorQuilometro.addTextChangedListener(this);
                        }
                    }
                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                edtTaxaPorMinuto.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    private String current = "";
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if(!s.toString().equals(current)) {
                            Locale myLocale = new Locale("pt", "BR");
                            edtTaxaPorMinuto.removeTextChangedListener(this);
                            String cleanString = s.toString().replaceAll("[R$,.]", "");
                            double parsed = Double.parseDouble(cleanString);
                            String formatted = NumberFormat.getCurrencyInstance(myLocale).format((parsed / 100));
                            current = formatted;
                            edtTaxaPorMinuto.setText(formatted);
                            edtTaxaPorMinuto.setSelection(formatted.length());

                            edtTaxaPorMinuto.addTextChangedListener(this);
                        }
                    }
                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                // Click listener para o botão confirmar localizado no Dialog
                btnConfirmar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String taxaPorQuilometroString = edtTaxaPorQuilometro.getText().toString().replaceAll("[R$,.]", "");
                        String taxaPorMinutoString = edtTaxaPorMinuto.getText().toString().replaceAll("[R$,.]", "");

                        taxaPorQuilometro = Double.parseDouble(taxaPorQuilometroString) / 100.00;
                        taxaPorMinuto = Double.parseDouble(taxaPorMinutoString) / 100.00;

//                        closeKeyboard();
                        dialog.dismiss();

                    }

//                    private void closeKeyboard() {
//                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                        imm.hideSoftInputFromWindow(dialog.getWindow().getDecorView().getWindowToken(), 0);
//                    }


                });

                dialog.show();
            }

        });
    }


    private void CalcularValorDaViagem() {
        Double total = 0.0;

        // Checando se o usuário traçou uma rota
        if (durationValue == 0 && distanceValue == 0) {
            msgToast("Trace uma rota para que seja possível calcular o custo da viagem");
        } else {
            double minutos = durationValue / 60.0;
            double quilometros = distanceValue / 1000.0;
            String valorTotal;

            total = (minutos * taxaPorMinuto) + (quilometros * taxaPorQuilometro);
            valorTotal = String.format("R$%.2f", total);
            txtPrecoTotal.setText(valorTotal);
//            System.out.println("Minutos: " + minutos + "\nDistancia: " + quilometros + "\nTaxa por minuto: " + taxaPorMinuto + "\nTaxa por quilometro:" + taxaPorQuilometro
//            + "\nTotal: " + total);
        }

    }

    private void TracaRota() {
        String origem = edtOrigem.getText().toString();
        String destino = edtDestino.getText().toString();

        if(origem.isEmpty()) {
            msgToast("Digite um endereço de origem");
            return;
        }

        if(destino.isEmpty()) {
            msgToast("Digite um endereço de destino");
            return;
        }
        //chamar metodo de direcao de rotas.
        try {
            new DirectionFinder(this, origem, destino).execute();
        } catch (UnsupportedEncodingException e) {
            msgToast("Erro na execuçao do Direction. " + e);
        }
    }

    private void msgToast (String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
//        LatLng UnPRF = new LatLng(-5.861216, -35.192672);
//        mMap.addMarker(new MarkerOptions().position(UnPRF).title("UnP Roberto Freire"));

//        mMap.moveCamera(CameraUpdateFactory.newLatLng(UnPRF));
    }

    @Override
    public void onDirectionFinderStart() {
        progressDialog = progressDialog.show(this, "Aguarde!", "Traçando rotas...", true);

        if(originMarker != null) {
            for (Marker origem: originMarker)
                origem.remove();
        }

        if(destinationMarker != null) {
            for (Marker destination: destinationMarker)
                destination.remove();
        }

        if(polylinePaths != null) {
            for (Polyline polyline: polylinePaths)
                polyline.remove();
        }

    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarker = new ArrayList<>();
        destinationMarker = new ArrayList<>();

        for(Route route: routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 15));
            txtDuracao.setText(route.duration.text);
            txtDistancia.setText(route.distance.text);

            distanceValue = route.distance.value;
            durationValue = route.duration.value;

            //marcadores
            originMarker.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .title(route.startAddress)
                    .position(route.startLocation)
            ));
            destinationMarker.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .title(route.endAddress)
                    .position(route.endLocation)
            ));

            //Polyline
            PolylineOptions polylineOptions = new PolylineOptions().geodesic(true)
                    .color(Color.argb(98, 0,51,255)).width(20);
            for (int i = 0; i < route.points.size(); i++) {
                polylineOptions.add(route.points.get(i));
            }
            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }
}
