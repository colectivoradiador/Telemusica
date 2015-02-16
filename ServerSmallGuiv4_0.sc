
+ Server {

	// splitting makeWindow and makeGui, makes sure that makeWindow can be overridden 
	// while maintaining the availability of the GUI server window
	
	addLongWindow { arg w, nameextra;
		this.addLongGui( w ,nameextra);
	}
	
	addLongGui { arg w,nameextra;
		var active, booter, killer, makeDefault, running, booting, stopped, bundling;
		var countsViews, ctlr;
		var dumping = false, label, gui, font,offset=44;
				
		gui = GUI.current;
		font = GUI.font.new("Helvetica", 10);
		
		StaticText(w, Rect(10, 10, 140, 18))
		.string_(name.asString+nameextra)
		.align_(\center)
		.font_(GUI.font.new("Helvetica", 14));
		
		StaticText(w, Rect(10, 10, 30, 18))
		.string_(" -> ")
		.align_(\right)
		.font_(GUI.font.new("Helvetica", 18));
		
		if(isLocal) {
			booter = gui.button.new(w, Rect(0,0, 20, 18));
			booter.canFocus = false;
			booter.font = font;
			booter.states = [["B", Color.black, Color.clear],
						   ["Q", Color.black, Color.green.alpha_(0.2)]];
			
			booter.action = { arg view; 
				if(view.value == 1, {
					booting.value;
					this.boot;
				});
				if(view.value == 0,{
					this.quit;
				});
			};
			booter.setProperty(\value,serverRunning.binaryValue);
			
			killer = gui.button.new(w, Rect(0,0, 20, 18));
			killer.states = [["K", Color.black, Color.clear]];
			killer.font = font;
			killer.canFocus = false;
			killer.action = { Server.killAll };	
			offset=0;
		};
		
		active = gui.staticText.new(w, Rect(0,0, 62+offset, 18));
		active.string = this.name.asString;
		active.align = \center;
		active.font = gui.font.new( gui.font.defaultSansFace, 12 ).boldVariant;
		active.background = Color.white;
		if(serverRunning,running,stopped);		
		
		w.view.keyDownAction = { arg view, char, modifiers;
			var startDump, stopDump, stillRunning;
			
				// if any modifiers except shift key are pressed, skip action
			if(modifiers & 16515072 == 0) {
				
				case 
				{char === $n } { this.queryAllNodes(false) }
				{char === $N } { this.queryAllNodes(true) }
				{char === $l } { this.tryPerform(\meter) }
				{char === $ } { if(serverRunning.not) { this.boot } }
				{char === $s and: { gui.stethoscope.isValidServer( this ) } } { 
					GUI.use( gui, { this.scope })}
				{char == $d } {
					if(this.isLocal or: { this.inProcess }) {
						stillRunning = {
							SystemClock.sched(0.2, { this.stopAliveThread });
						};
						startDump = { 
							this.dumpOSC(1);
							this.stopAliveThread;
							dumping = true;
							w.name = "dumping osc: " ++ name.asString;
							CmdPeriod.add(stillRunning);
						};
						stopDump = {
							this.dumpOSC(0);
							this.startAliveThread;
							dumping = false;
							w.name = label;
							CmdPeriod.remove(stillRunning);
						};
						if(dumping, stopDump, startDump)
					} {
						"cannot dump a remote server's messages".inform
					}
				
				}
				{char === $m } { if(this.volume.isMuted) { this.unmute } { this.mute } }

			};
		};
		
		if (isLocal, {
			
			running = {
				active.stringColor_(Color.new255(74, 120, 74));
				active.string = "running";
				booter.setProperty(\value,1);
			};
			stopped = {
				active.stringColor_(Color.grey(0.3));
				active.string = "inactive";
				booter.setProperty(\value,0);
				countsViews.do(_.string = "");
			};
			booting = {
				active.stringColor_(Color.new255(255, 140, 0));
				active.string = "booting";
				//booter.setProperty(\value,0);
			};
			bundling = {
				active.stringColor_(Color.new255(237, 157, 196));
				booter.setProperty(\value,1);
			};
			
			w.onClose_(w.onClose.addFunc {
				ctlr.remove;
			}
			);
		},{	
			running = {
				active.stringColor_(Color.new255(74, 120, 74));
				active.string = "running";
				active.background = Color.white;
			};
			stopped = {
				active.stringColor_(Color.grey(0.3));
				active.string = "inactive";

			};
			booting = {
				active.stringColor_(Color.new255(255, 140, 0));
				active.string = "booting";
			};
			
			bundling = {
				active.stringColor = Color.new255(237, 157, 196);
				active.background = Color.red(0.5);
				booter.setProperty(\value,1);
			};
			
			w.onClose_(w.onClose.addFunc {
				// but do not remove other responders
				this.stopAliveThread;
				ctlr.remove;
			});
		});

		if(serverRunning,running,stopped);
			
		countsViews = 
		#[
			"CPU :", "Synths :", "SynthDefs :"
		].collect { arg name, i;
			var label,numView, pctView;
			label = gui.staticText.new(w, Rect(0,0,55, 18));
			label.string = name;
			label.font = font;
			label.align = \right;
		
			if (i < 1, { 
				numView = gui.staticText.new(w, Rect(0,0,20, 18));
				numView.font = font;
				numView.align = \left;
			
				pctView = gui.staticText.new(w, Rect(0,0, 12, 18));
				pctView.string = "%";
				pctView.font = font;
				pctView.align = \left;
			},{
				numView = gui.staticText.new(w, Rect(0,0, 20, 18));
				numView.font = font;
				numView.align = \left;
			});
			
			numView
		};

 		w.front;

		ctlr = SimpleController(this)
			.put(\serverRunning, {	if(serverRunning,running,stopped) })
			.put(\counts,{
				countsViews.at(0).string = avgCPU.round(0.1);
				countsViews.at(1).string = numSynths;
				countsViews.at(2).string = numSynthDefs;
			})
			.put(\bundling, bundling);
			
		this.startAliveThread;
	}
}
