package com.retain;

public interface DeleteHandlerInterface
{
	public static interface OnDeleteItemListener
	{
		public abstract void onDeleteItem(long rowId);
	}
}
