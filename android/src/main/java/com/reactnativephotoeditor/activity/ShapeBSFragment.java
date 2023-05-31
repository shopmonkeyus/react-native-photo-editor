package com.reactnativephotoeditor.activity;

import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.reactnativephotoeditor.R;

import java.util.ArrayList;

import ja.burhanrashid52.photoeditor.shape.ShapeType;

public class ShapeBSFragment extends BottomSheetDialogFragment implements SeekBar.OnSeekBarChangeListener {
    private String shapeText = "Shape";
    private String brushText = "Brush";
    private String opacityText = "Opacity";
    private String lineText = "Line";
    private String ovalText = "Oval";
    private String rectangleText = "Rectangle";

    public ShapeBSFragment(ArrayList<String> translations) {
        if (translations.size() == 6) {
            shapeText = translations.get(0);
            brushText = translations.get(1);
            opacityText = translations.get(2);
            lineText = translations.get(3);
            ovalText = translations.get(4);
            rectangleText = translations.get(5);
        }
    }

    private Properties mProperties;

    public interface Properties {
        void onColorChanged(int colorCode);

        void onOpacityChanged(int opacity);

        void onShapeSizeChanged(int shapeSize);

        void onShapePicked(ShapeType shapeType);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bottom_shapes_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView rvColor = view.findViewById(R.id.shapeColors);
        SeekBar sbOpacity = view.findViewById(R.id.shapeOpacity);
        SeekBar sbBrushSize = view.findViewById(R.id.shapeSize);
        RadioGroup shapeGroup = view.findViewById(R.id.shapeRadioGroup);
        TextView shapeTextView = view.findViewById(R.id.shapeType);
        TextView brushTextView = view.findViewById(R.id.txtShapeSize);
        TextView opacityTextView = view.findViewById(R.id.txtOpacity);
        RadioButton brushRadio = view.findViewById(R.id.brushRadioButton);
        RadioButton lineRadio = view.findViewById(R.id.lineRadioButton);
        RadioButton ovalRadio = view.findViewById(R.id.ovalRadioButton);
        RadioButton rectRadio = view.findViewById(R.id.rectRadioButton);

        // Custom translations
        shapeTextView.setText(shapeText);
        brushTextView.setText(brushText);
        opacityTextView.setText(opacityText);
        brushRadio.setText(brushText);
        lineRadio.setText(lineText);
        ovalRadio.setText(ovalText);
        rectRadio.setText(rectangleText);

        // shape picker
        shapeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.lineRadioButton) {
                mProperties.onShapePicked(ShapeType.LINE);
            } else if (checkedId == R.id.ovalRadioButton) {
                mProperties.onShapePicked(ShapeType.OVAL);
            } else if (checkedId == R.id.rectRadioButton) {
                mProperties.onShapePicked(ShapeType.RECTANGLE);
            } else {
                mProperties.onShapePicked(ShapeType.BRUSH);
            }
        });

        sbOpacity.setOnSeekBarChangeListener(this);
        sbBrushSize.setOnSeekBarChangeListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        rvColor.setLayoutManager(layoutManager);
        rvColor.setHasFixedSize(true);
        ColorPickerAdapter colorPickerAdapter = new ColorPickerAdapter(getActivity());
        colorPickerAdapter.setOnColorPickerClickListener(colorCode -> {
            if (mProperties != null) {
                dismiss();
                mProperties.onColorChanged(colorCode);
            }
        });
        rvColor.setAdapter(colorPickerAdapter);
    }

    public void setPropertiesChangeListener(Properties properties) {
        mProperties = properties;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
      int id = seekBar.getId();
      if (id == R.id.shapeOpacity) {
        if (mProperties != null) {
          mProperties.onOpacityChanged(i);
        }
      } else if (id == R.id.shapeSize) {
        if (mProperties != null) {
          mProperties.onShapeSizeChanged(i);
        }
      }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
