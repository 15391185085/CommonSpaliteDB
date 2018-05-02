package com.ieds.gis.base.test.page;

import java.util.UUID;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.ieds.gis.base.BaseActivity;
import com.ieds.gis.base.db.R;
import com.ieds.gis.base.test.module.defect.service.DefectService;

public class TestMainActivity extends BaseActivity {
	private DefectService ds;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		final String id = UUID.randomUUID().toString();
		this.setContentView(R.layout.test_layout);
		Button b1 = (Button) this.findViewById(R.id.b1);
		Button b2 = (Button) this.findViewById(R.id.b2);
		ds = new DefectService(this);
		b1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ds.openEditPage(id);
			}
		});

		b2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ds.openBrowsePage(id);
			}
		});
	}
}
