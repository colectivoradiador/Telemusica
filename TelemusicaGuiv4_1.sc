
TelemusicaGui{

	var <server, <address, <bCaster;
	var win,values;
	var relay;

    *new{ |svrArr, addrArr|
    		^super.new.init(svrArr, addrArr);
	}

	init{

		//variables relacionadas con el relay de datos
		relay=(
		on:false,
		address:List.new,
		ready:true,
		timer:TempoClock(2)
		);
		//sirve para limitar el numero de mensajes que manda por segundo
		relay[\timer].schedAbs(1, { relay[\ready]=true; 1 });
		//cierra el timer cuando se cierra superCollider
		ServerQuit.({ relay[\timer].clear },\default);

		values =(\first:true);
    		server = IdentityDictionary();
    		address = IdentityDictionary();

    	win=Window.new(" Tele-Servers ",Rect(0,800,550,4),true).front; // cambiar esto para la versión Qt
    		win.view.decorator = FlowLayout(win.view.bounds,gap:2@4);

	}


	addUser{|array,dir, port= 57110,loc="Amsterdam"|
		var addr,svr,name,look=false;
		if(size(array) > 0){

		win.setInnerExtent(win.bounds.width,win.bounds.height+(22*size(array)));
			//win.resizeTo(win.bounds.width,win.bounds.height+(22*size(array)));


			array.do({|item|


			name=item[0];
			dir=item[1];
			if( size(item) > 2 ){port=item[2]}{port=57110};

			addr = NetAddr(dir, port);

			address.put(name,addr);

			svr = Server(name.asSymbol, addr);
			server.put(name,svr);

			svr.addLongWindow(win,format("( % )",addr.port));

				win.view.decorator.nextLine;

				if(values[\first]){
				values.put(\name,name.asString);
				values.put(\location,loc.asString);
				//guardamos esta variable para comodidad
				values.put(\port,addr.port);
				values[\first]=false;
				};

			});

		}
		{

		name=array;

		addr = NetAddr(dir, port);
		address.put(name,addr);

		svr = Server(name.asSymbol, addr);
		server.put(name,svr);

		win.setInnerExtent(win.bounds.width,win.bounds.height+22);
		svr.addLongWindow(win,format("( % )",addr.port));
		win.view.decorator.nextLine;

		if(values[\first]){
				values.put(\name,name.asString);
				values.put(\location,loc.asString);
				//guardamos esta variable para comodidad
				values.put(\port,addr.port);
				values[\first]=false;

				};

		}

	}


	addRelayUser{|dir,port|
		relay[\on]=true;
		relay[\address].add(NetAddr.new(dir,port));
		this.addRelayGui(dir,port);

	}

	relayOff{
	relay[\on]=false;

	}

	relayOn{
	relay[\on]=true;

	}

	startBroadcast{
	var array=address.asArray;

	bCaster = BroadcastServer(\TeleMusica, array[0], nil, 0).addresses_(array).makeWindow;
	if(values[\locationId].isNil){
		this.addLocationId(0);
	};
	this.relayUserInfo();

	}

	//manda informacion de un usuario
	relayUserInfo{
		if(relay[\on]){
		relay[\address].do({|item|
			item.sendMsg("/addUser",values[\locationId],values[\name],values[\port],
			"TeleMusica conectado, empezando transmision" );
		});
		}
	}

	//manda informacion de una locacion
	relayLocationInfo{|id|
		if(relay[\on]){
		relay[\address].do({|item|
			item.sendMsg("/addLoc",values[\location],id);
		});
		}
	}
	//vulev a mandar informacion relevante
	relayAllInfo{
	this.relayLocationInfo(values[\locationId]);
	this.relayUserInfo();
	}

	printIp{|name|
	postln(" ");
	if(name.isNil){
	(address.keys).do({|item,i|
	post("[ "++item+" , ");
	post(address[item].ip+" , ");
	postln(address[item].port+" ]");
	});
	}
	{
	post("[ "++name+" , ");
	post(address[name].ip+" , ");
	postln(address[name].port+" ]");
	};

	}

	ip{|nombre|
	^address[nombre].ip
	}

	port{|nombre|
	^address[nombre].port
	}

	castSynth{|synthDef|
	post("Sinte "++synthDef.name++" mandado a: ");
		 server.do({|item|
	 				post(item);
	 				post(" ");
	 				synthDef.send(item)
	 			});
	 	postln(" ");

	if(relay[\on]){this.sendRelayMsg("/d_recv",synthDef.name);}

	 }

	newSynth{|name,id,values=nil|
		if(values.isNil)
		{
		bCaster.sendMsg(\s_new,name,id);
		if(relay[\on]){
			this.sendRelayMsgStr("/s_new",id,name," ");
			}
		}
		{
		bCaster.sendBundle(bCaster.latency, [\s_new,name,id, 0, 1] ++ values);
		if(relay[\on]){
		this.sendRelayMsgStr("/s_new",id,name,values.asString)
		}
		};

	}

	set{|id,parameter,parametervalue|
		if(size(parameter) == 0)
		{bCaster.sendMsg(\n_set,id,parameter,parametervalue);
		if(relay[\on]){
			this.sendRelayMsgFloat("/n_set",id, parameter,parametervalue)			}

			}
		{bCaster.sendBundle(bCaster.latency,[\n_set,id]++parameter);
		if(relay[\on]){
			//tenemos que mandar uno por cada parametro
			(parameter.size/2).do({|i|
			this.sendRelayMsgFloat("/n_set",id,parameter[i*2],parameter[(i*2) +1].asFloat);
			});

			}
			};

		//le da tiempo de descanso al relay;
		relay[\ready]=false;
	}

//------------------------------------
	allocBuff{|dur,chan = 1, bufnum = 10|
		var buf;
		server.do({|item|
			post(item);
			post(" ");
			//synthDef.send(item)
			buf = Buffer.alloc(item, dur, numChannels: chan, bufnum: bufnum);
			post("alojó un buffer con bufnum: "++buf.bufnum++" en el servidor de: ");
});
postln(" ");

	if(relay[\on]){this.sendRelayMsg("/d_recv", buf.bufnum);}
^buf;
}


  sendSamp{|buf,bufAlloc|
 // para mandar samples en tiempo real
	buf.getToFloatArray(wait: 0.01,action:{arg array; bufAlloc.sendCollection(array, action: {arg kk; "finished".postln;})},timeout: 150);
		}

	freeBufs{|ser|
	ser.cachedBuffersDo({ |buf| buf.free;buf.postln  }); //buf.free
      }

	free{|id|
	bCaster.sendMsg(\n_free,id);
			if(relay[\on]){
			this.sendRelayMsgFloat("/n_free",id," ",0);
			}

	}

	release{|id|
	bCaster.sendMsg(\n_set,id,\gate,0);

			if(relay[\on]){
			this.sendRelayMsgFloat("/n_release",id," ",0);
			}

	}

	//el mensaje mas simple a mandar, solo es un string
	sendRelayMsg{|cmd="/string",string|
	relay[\address].do({|item|
		item.sendMsg(cmd,values[\locationId],values[\port],string);
	});

	}

	//manda informacion de un comando, el id de locacion, puerto, id de sinte,string, y string extra
	sendRelayMsgStr{|cmd="/string",id,string,stringextra|
	relay[\address].do({|item|
		item.sendMsg(cmd,values[\locationId],values[\port],id,string,stringextra);
	});


	}


	//manda informacion de un comando, el id de locacion, puerto, id de sinte,string, y un float
	sendRelayMsgFloat{|cmd="/string",id,string,value|
	relay[\address].do({|item|
		item.sendMsg(cmd,values[\locationId],values[\port],id,string,value.asFloat);
	});

	}

	addRelayGui{|ip,port|
	win.setInnerExtent(win.bounds.width,win.bounds.height+22);
	//	win.resizeTo(win.bounds.width,win.bounds.height+22);

	StaticText(win,500@18)
	.font_(Font("Monaco",15))
	.background_(Color.rand)
	.string_(format("  Relay ip: %    port: %",ip,port));
	}
	//agrega una locacion con id
	addLocationId{|id|
	values.put(\locationId,id);
	this.relayLocationInfo(id);
	}



}

                                                                                                                   