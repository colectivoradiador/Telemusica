+SynthDesc {
	
	makeTeleWindow{|tele,id=1101|
		^this.makeTeleGui(tele,id);
	}
	
	
makeTeleGui{|tele,id|
		var w, teleMus, startButton, sliders,boxes;
		var cmdPeriodFunc;
		var usefulControls, numControls,audioControls,idBox;
		var getSliderValues, gui,auxcolor,font,fontColor,count;
		var controles;
		controles=();
		teleMus = tele;
		
		gui = GUI.current;
		
		font=Font("Helvetica",10);
		
		count=0;
		audioControls=List.new();
		usefulControls = controls.select {|controlName, i|
			var ctlname,audioFound=false;
			ctlname = controlName.name.asString;
			((ctlname == "out") || (ctlname == "fade") || (ctlname == "in")).if({audioControls.add(controlName);},{audioFound=true});
			( (ctlname != "in") && audioFound && (msgFuncKeepGate or: { ctlname != "gate" }))
			
		};

		numControls = usefulControls.size;
		sliders = Array.newClear(numControls);
		boxes=Array.newClear(audioControls.size);
		
		// make the window
		auxcolor=Color.rand;
		auxcolor=[[auxcolor,auxcolor.complementary,Color.black],[auxcolor,Color.black,Color.white],[auxcolor,Color.white,Color.black],[Color.black,auxcolor,Color.white],[Color.white,auxcolor,Color.black]].choose;
		fontColor=auxcolor[2];
		
		
		
		w = gui.window.new("-- Tele:"+name+"--", Rect(20, 400, 410, numControls * 18 + 28),true);
		w.view.decorator = FlowLayout(w.view.bounds);
		
		w.view.background=Gradient(auxcolor[0],auxcolor[1],\h,12);
		
		
		// add a button to start and stop the sound.
		startButton = gui.button.new(w, 60 @ 15);
		startButton.states = [
			["Start", Color.black, Color.green],
			["Stop", Color.white, Color.red]
		];
		startButton.font_(font);
		
		getSliderValues = {
			var envir;
			envir = ();
			usefulControls.do {|controlName, i|
				var ctlname;
				ctlname = controlName.name.asSymbol;
				envir.put(ctlname, sliders[i].value);
			};
			envir.use {
				msgFunc.valueEnvir
			};
		};
		
		idBox=gui.ezNumber.new(w,90 @ 15,\ID,[1001,9999,\lin,1,1101],{|ez| if(ez.enabled){id=ez.value}{ez.value=id};},id)
		.font_(font)
		.setColors(stringColor:fontColor);

		startButton.action = {|view|
				if (view.value == 1) {
					// start sound
					teleMus.newSynth(name,id,getSliderValues.value);
					idBox.enabled_(false);
				};
				if (view.value == 0) {
					if (this.hasGate) {
						// set gate to zero to cause envelope to release
					teleMus.release(id);
					}{
					teleMus.free(id);
					};
					idBox.enabled_(true);
				};
		};
		


		audioControls.do{|controlName,i|
		var ctlname, ctlname2, capname, spec;
			ctlname = controlName.name;
			capname = ctlname.copy; 
			capname[0] = capname[0].toUpper;
			
			ctlname = ctlname.asSymbol;
			if((spec = metadata.tryPerform(\at, \specs).tryPerform(\at, ctlname)).notNil) {
				spec = spec.asSpec
			} {
				spec = ctlname.asSpec;
			};
			if (spec.notNil) {
				boxes[i] = gui.ezNumber.new(w,85 @ 15, capname, spec, 
					{ |ez| 
						
					 teleMus.set(id,ctlname,ez.value);
					 	
					}, controlName.defaultValue);
				boxes[i].font_(font);
				boxes[i].setColors(stringColor:fontColor);
			} {postln(controlName.name+" is not present in metadata");
				count=count+1;
			}
		};
		
		// create controls for all parameters
		usefulControls.do {|controlName, i|
			var ctlname, ctlname2, capname, spec;
			ctlname = controlName.name;
			capname = ctlname.copy; 
			capname[0] = capname[0].toUpper;
			
			ctlname = ctlname.asSymbol;
			if((spec = metadata.tryPerform(\at, \specs).tryPerform(\at, ctlname)).notNil) {
				spec = spec.asSpec
			} {
				spec = ctlname.asSpec;
			};
			if (spec.notNil) {
				sliders[i] = gui.ezSlider.new(w, 400 @ 15, capname, spec, 
					{ |ez| 	
					 teleMus.set(id,ctlname,ez.value);
					 
					}, controlName.defaultValue);
				sliders[i].font_(font);
				sliders[i].setColors(stringColor:fontColor);
				w.view.decorator.nextLine;	
			} {postln(controlName.name+" is not present in metadata");
				count=count+1;
			}
		};
			
		//todos los sliders los metemos a controles
		sliders.do {|item, i|
			var ctlname;
			if(sliders[i].notNil){
			ctlname = usefulControls[i].name;
			controles.put(ctlname,item);
			}
			};
		//tambien la caja de prende y apaga	
		controles.put(\onoff,startButton);
		
			
		// set start button to zero upon a cmd-period
		cmdPeriodFunc = { startButton.value = 0; };
		CmdPeriod.add(cmdPeriodFunc);
		
		// stop the sound when window closes and remove cmdPeriodFunc.
		w.onClose = {
			
			teleMus.free(id);

			CmdPeriod.remove(cmdPeriodFunc);
		};
		(count > 0).if({w.bounds_(Rect(20, 400, 410, (numControls-count) * 18 + 28))});
		w.front; // make window visible and front window.
		tele.sendRelayMsg("/string", format(" TeleSynth Gui creado con id %",id));

		^controles;
	}

}
	
	
	