document.addEventListener('error',event=>{
  if(event.target instanceof HTMLImageElement&&event.target.closest('.profile-avatar,.chat-avatar'))event.target.remove();
},true);
